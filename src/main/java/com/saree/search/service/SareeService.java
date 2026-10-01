package com.saree.search.service;

import com.saree.search.dto.*; import com.saree.search.entity.Saree; import com.saree.search.repository.SareeRepository;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.nio.file.*; import java.util.*;

@Service
public class SareeService {
  private final SareeRepository repository; private final ColorDetectionService detector;
  @Value("${saree.match.threshold:70}") private double threshold;
  @Value("${saree.upload.max-bytes:10485760}") private long maxBytes;
  public SareeService(SareeRepository repository, ColorDetectionService detector) { this.repository=repository; this.detector=detector; }
  public SareeMatchResponse search(MultipartFile image) throws IOException {
    validate(image); Path temp = Files.createTempFile("saree-search-", suffix(image));
    try { image.transferTo(temp); List<ColorInfo> colors = detector.detect(temp);
      List<SareeMatch> matches = repository.findAll().stream().map(s -> new AbstractMap.SimpleEntry<>(s, similarity(colors, s)))
        .filter(e -> e.getValue() >= threshold).sorted((a,b)->Double.compare(b.getValue(),a.getValue()))
        .map(e -> new SareeMatch(e.getKey().getId(), e.getKey().getImageName(), e.getKey().getImagePath(), round(e.getValue()), e.getKey().getPrimaryColor())).toList();
      return new SareeMatchResponse(!matches.isEmpty(), matches.size(), matches.isEmpty() ? "No matching saree found in the database." : null, colors, matches);
    } finally { Files.deleteIfExists(temp); }
  }
  public List<Saree> all() { return repository.findAll(); } public Saree byId(Long id) { return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Saree not found.")); }
  private void validate(MultipartFile f) {
    if (f == null || f.isEmpty()) throw new IllegalArgumentException("Please choose an image to search.");
    if (f.getSize() > maxBytes) throw new IllegalArgumentException("Image must be 10 MB or smaller.");
    String n=Optional.ofNullable(f.getOriginalFilename()).orElse("").toLowerCase(); if (!(n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".png"))) throw new IllegalArgumentException("Please upload a valid JPG, JPEG, or PNG image.");
  }
  private String suffix(MultipartFile f) { String n=Optional.ofNullable(f.getOriginalFilename()).orElse(".jpg"); return n.substring(n.lastIndexOf('.')); }
  private double similarity(List<ColorInfo> query, Saree s) {
    List<ColorInfo> stored = new ArrayList<>(); stored.add(new ColorInfo(s.getPrimaryColor(),s.getPrimaryColorPercentage())); if(s.getSecondaryColor()!=null) stored.add(new ColorInfo(s.getSecondaryColor(),s.getSecondaryColorPercentage()));
    double weighted=0, covered=0; for (ColorInfo q:query) { double best=stored.stream().mapToDouble(d -> colourCloseness(q.color(),d.color()) * (1-Math.min(1,Math.abs(q.percentage()-d.percentage())/100))).max().orElse(0); weighted += q.percentage()*best; covered+=q.percentage(); }
    return covered == 0 ? 0 : weighted/covered;
  }
  private double colourCloseness(String a,String b) { if(a.equals(b))return 100; Map<String,Integer> h=Map.ofEntries(Map.entry("RED",0),Map.entry("MAROON",175),Map.entry("PINK",175),Map.entry("ORANGE",15),Map.entry("GOLD",20),Map.entry("YELLOW",30),Map.entry("GREEN",60),Map.entry("DARK_GREEN",65),Map.entry("BLUE",100),Map.entry("NAVY_BLUE",120),Map.entry("PURPLE",140),Map.entry("VIOLET",155),Map.entry("BROWN",10),Map.entry("BEIGE",22),Map.entry("CREAM",28),Map.entry("BLACK",0),Map.entry("WHITE",0),Map.entry("GREY",0),Map.entry("SILVER",0)); if(Set.of("BLACK","WHITE","GREY","SILVER").contains(a)||Set.of("BLACK","WHITE","GREY","SILVER").contains(b)) return 15; int d=Math.abs(h.get(a)-h.get(b)); d=Math.min(d,180-d); return Math.max(0,100-d*1.35); }
  private double round(double v) { return Math.round(v*10)/10.0; }
}
