package com.saree.search.controller;

import com.saree.search.dto.SareeMatchResponse;
import com.saree.search.entity.Saree;
import com.saree.search.service.SareeImageIndexer;
import com.saree.search.service.SareeService;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import com.saree.search.dto.*;
import java.io.IOException; import java.util.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/sarees")
public class SareeController {
  private final SareeService sarees; private final SareeImageIndexer indexer;
  @Value("${saree.admin.token}") private String adminToken;
  @Value("${saree.image.directory:./saree-images}") private String imageDirectory;
  public SareeController(SareeService sarees, SareeImageIndexer indexer) { this.sarees=sarees; this.indexer=indexer; }
  @PostMapping(value="/search", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public SareeMatchResponse search(@RequestParam("image") MultipartFile image,
    @RequestParam(required=false) String sareeType,@RequestParam(required=false) String fabric,@RequestParam(required=false) String pattern,@RequestParam(required=false) String occasion,@RequestParam(required=false) Double maxPrice,@RequestParam(defaultValue="false") Boolean inStockOnly,@RequestParam(defaultValue="balanced") String colourMode) throws IOException { return sarees.search(image,new SearchFilters(sareeType,fabric,pattern,occasion,maxPrice,inStockOnly,colourMode)); }
  @GetMapping public List<Saree> all() { return sarees.all(); }
  @GetMapping("/{id}") public Saree one(@PathVariable Long id) { return sarees.byId(id); }
  @PostMapping("/reindex") public Map<String,Object> reindex(@RequestHeader("X-Admin-Token") String token) throws IOException { authorize(token); int indexed=indexer.reindex(); return Map.of("message","Image index rebuilt successfully.","indexed",indexed); }
  @GetMapping("/{id}/similar") public SareeMatchResponse similar(@PathVariable Long id,@RequestParam(defaultValue="swap") String colourMode) { return sarees.similar(id,colourMode); }
  @PutMapping("/{id}/metadata") public Saree metadata(@PathVariable Long id,@RequestHeader("X-Admin-Token") String token,@Valid @RequestBody SareeMetadataRequest request) { authorize(token); return sarees.updateMetadata(id,request); }
  @GetMapping("/evaluation/report") public EvaluationReport evaluation() { return sarees.evaluation(); }
  @PostMapping(value="/admin/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Saree upload(@RequestHeader("X-Admin-Token") String token,@RequestParam("image") MultipartFile image,@RequestParam(required=false) String folder,@RequestParam String sareeType,@RequestParam String fabric,@RequestParam String pattern,@RequestParam(required=false) String occasion,@RequestParam(required=false) Double price) throws IOException {
    authorize(token); String original=Optional.ofNullable(image.getOriginalFilename()).orElse("");
    if(!original.toLowerCase().matches(".*\\.(jpg|jpeg|png)$")) throw new IllegalArgumentException("Only JPG, JPEG or PNG files are allowed.");
    String safeName=Path.of(original).getFileName().toString().replaceAll("[^A-Za-z0-9._-]","_"); String safeFolder=Optional.ofNullable(folder).orElse("uploads").replaceAll("[^A-Za-z0-9_-]","_");
    Path root=Path.of(imageDirectory).toAbsolutePath().normalize(), destination=root.resolve(safeFolder).resolve(UUID.randomUUID()+"-"+safeName).normalize();
    if(!destination.startsWith(root)) throw new IllegalArgumentException("Invalid upload folder."); Files.createDirectories(destination.getParent()); image.transferTo(destination);
    Saree s=indexer.indexUploaded(destination); return sarees.updateMetadata(s.getId(),new SareeMetadataRequest(sareeType,fabric,pattern,occasion,price,true));
  }
  private void authorize(String token) { if(token==null||token.isBlank()||!token.equals(adminToken)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"A valid X-Admin-Token is required."); }
}
