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
  public SareeMatchResponse search(MultipartFile image, SearchFilters filters) throws IOException {
    validate(image); Path temp = Files.createTempFile("saree-search-", suffix(image));
    try { image.transferTo(temp); VisualAnalysis analysis = detector.analyse(temp);
      List<SareeMatch> matches = repository.findAll().stream().filter(s -> accepts(s, filters)).map(s -> toMatch(s, analysis, filters))
        .filter(m -> m.similarity() >= threshold).sorted((a,b)->Double.compare(b.similarity(),a.similarity())).limit(48).toList();
      return new SareeMatchResponse(!matches.isEmpty(), matches.size(), matches.isEmpty() ? "No matching saree found with these filters." : null, analysis.colors(), analysis.foregroundCoverage(), analysis.patternHint(), matches);
    } finally { Files.deleteIfExists(temp); }
  }
  public SareeMatchResponse similar(Long id, String colourMode) {
    Saree source=byId(id); double[] signature=parse(source.getDesignSignature());
    SearchFilters filters=new SearchFilters(null,null,null,null,null,false,colourMode);
    List<SareeMatch> matches=repository.findAll().stream().filter(s->!s.getId().equals(id)).filter(s->accepts(s,filters))
      .map(s->toMatch(s,new VisualAnalysis(List.of(new ColorInfo(source.getPrimaryColor(),source.getPrimaryColorPercentage())),signature,100,"catalogue design"),filters))
      .sorted((a,b)->Double.compare(b.similarity(),a.similarity())).limit(24).toList();
    return new SareeMatchResponse(!matches.isEmpty(),matches.size(),matches.isEmpty()?"No similar sarees found.":null,List.of(new ColorInfo(source.getPrimaryColor(),source.getPrimaryColorPercentage())),100,"catalogue design",matches);
  }
  public Saree updateMetadata(Long id, SareeMetadataRequest data) { Saree s=byId(id); s.setSareeType(data.sareeType()); s.setFabric(data.fabric()); s.setPattern(data.pattern()); s.setOccasion(data.occasion()); s.setPrice(data.price()); if(data.inStock()!=null)s.setInStock(data.inStock()); return repository.save(s); }
  public EvaluationReport evaluation() { List<Saree> all=repository.findAll(); long labelled=all.stream().filter(s->s.getPattern()!=null&&!s.getPattern().isBlank()).count(); long signed=all.stream().filter(s->s.getDesignSignature()!=null&&!s.getDesignSignature().isBlank()).count(); return new EvaluationReport(all.size(),(int)labelled,(int)signed,all.isEmpty()?0:round(labelled*100.0/all.size()),"Use labelled query/result pairs for Top-1 and Top-5 relevance as your catalogue grows."); }
  public List<Saree> all() { return repository.findAll(); } public Saree byId(Long id) { return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Saree not found.")); }
  private void validate(MultipartFile f) {
    if (f == null || f.isEmpty()) throw new IllegalArgumentException("Please choose an image to search.");
    if (f.getSize() > maxBytes) throw new IllegalArgumentException("Image must be 10 MB or smaller.");
    String n=Optional.ofNullable(f.getOriginalFilename()).orElse("").toLowerCase(); if (!(n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".png"))) throw new IllegalArgumentException("Please upload a valid JPG, JPEG, or PNG image.");
  }
  private String suffix(MultipartFile f) { String n=Optional.ofNullable(f.getOriginalFilename()).orElse(".jpg"); return n.substring(n.lastIndexOf('.')); }
  private SareeMatch toMatch(Saree s, VisualAnalysis analysis, SearchFilters filters) {
    double visual=cosine(analysis.signature(),parse(s.getDesignSignature()))*100;
    double colour=similarity(analysis.colors(),s);
    boolean colorSwap="swap".equalsIgnoreCase(filters==null?null:filters.colourMode());
    double score=colorSwap ? visual : visual*.72+colour*.28;
    String why="Design/texture similarity " + round(visual) + "%" + (colorSwap ? "; colour deliberately ignored" : "; colour similarity " + round(colour) + "%");
    return new SareeMatch(s.getId(),s.getImageName(),s.getImagePath(),round(score),s.getPrimaryColor(),s.getSareeType(),s.getFabric(),s.getPattern(),s.getOccasion(),s.getPrice(),s.isInStock(),why);
  }
  private boolean accepts(Saree s, SearchFilters f) { if(f==null)return true; return equalsOrAny(f.sareeType(),s.getSareeType())&&equalsOrAny(f.fabric(),s.getFabric())&&equalsOrAny(f.pattern(),s.getPattern())&&equalsOrAny(f.occasion(),s.getOccasion())&&(f.maxPrice()==null||s.getPrice()==null||s.getPrice()<=f.maxPrice())&&(!Boolean.TRUE.equals(f.inStockOnly())||s.isInStock()); }
  private boolean equalsOrAny(String requested,String actual){return requested==null||requested.isBlank()||"any".equalsIgnoreCase(requested)||requested.equalsIgnoreCase(actual==null?"":actual);}
  private double[] parse(String value) { if(value==null||value.isBlank()) return new double[0]; return Arrays.stream(value.split(",")).mapToDouble(Double::parseDouble).toArray(); }
  private double cosine(double[] a,double[] b){if(a.length==0||b.length==0||a.length!=b.length)return 0;double sum=0;for(int i=0;i<a.length;i++)sum+=a[i]*b[i];return Math.max(0,sum);}
  private double similarity(List<ColorInfo> query, Saree s) {
    List<ColorInfo> stored = new ArrayList<>(); stored.add(new ColorInfo(s.getPrimaryColor(),s.getPrimaryColorPercentage())); if(s.getSecondaryColor()!=null) stored.add(new ColorInfo(s.getSecondaryColor(),s.getSecondaryColorPercentage()));
    double weighted=0, covered=0; for (ColorInfo q:query) { double best=stored.stream().mapToDouble(d -> colourCloseness(q.color(),d.color()) * (1-Math.min(1,Math.abs(q.percentage()-d.percentage())/100))).max().orElse(0); weighted += q.percentage()*best; covered+=q.percentage(); }
    return covered == 0 ? 0 : weighted/covered;
  }
  private double colourCloseness(String a,String b) { if(a.equals(b))return 100; Map<String,Integer> h=Map.ofEntries(Map.entry("RED",0),Map.entry("MAROON",175),Map.entry("PINK",175),Map.entry("ORANGE",15),Map.entry("GOLD",20),Map.entry("YELLOW",30),Map.entry("GREEN",60),Map.entry("DARK_GREEN",65),Map.entry("BLUE",100),Map.entry("NAVY_BLUE",120),Map.entry("PURPLE",140),Map.entry("VIOLET",155),Map.entry("BROWN",10),Map.entry("BEIGE",22),Map.entry("CREAM",28),Map.entry("BLACK",0),Map.entry("WHITE",0),Map.entry("GREY",0),Map.entry("SILVER",0)); if(Set.of("BLACK","WHITE","GREY","SILVER").contains(a)||Set.of("BLACK","WHITE","GREY","SILVER").contains(b)) return 15; int d=Math.abs(h.get(a)-h.get(b)); d=Math.min(d,180-d); return Math.max(0,100-d*1.35); }
  private double round(double v) { return Math.round(v*10)/10.0; }
}
