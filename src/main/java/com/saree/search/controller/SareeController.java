package com.saree.search.controller;

import com.saree.search.dto.SareeMatchResponse;
import com.saree.search.entity.Saree;
import com.saree.search.service.SareeImageIndexer;
import com.saree.search.service.SareeService;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.util.*;

@RestController
@RequestMapping("/api/sarees")
public class SareeController {
  private final SareeService sarees; private final SareeImageIndexer indexer;
  public SareeController(SareeService sarees, SareeImageIndexer indexer) { this.sarees=sarees; this.indexer=indexer; }
  @PostMapping(value="/search", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public SareeMatchResponse search(@RequestParam("image") MultipartFile image) throws IOException { return sarees.search(image); }
  @GetMapping public List<Saree> all() { return sarees.all(); }
  @GetMapping("/{id}") public Saree one(@PathVariable Long id) { return sarees.byId(id); }
  @PostMapping("/reindex") public Map<String,Object> reindex() throws IOException { int indexed=indexer.reindex(); return Map.of("message","Image index rebuilt successfully.","indexed",indexed); }
}
