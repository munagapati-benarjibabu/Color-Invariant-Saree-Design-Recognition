package com.saree.search.service;

import com.saree.search.dto.ColorInfo;
import com.saree.search.dto.VisualAnalysis;
import com.saree.search.entity.Saree;
import com.saree.search.repository.SareeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException; import java.nio.file.*; import java.util.*; import java.util.stream.Stream;

@Service
public class SareeImageIndexer {
  private final SareeRepository repository; private final ColorDetectionService detector;
  @Value("${saree.image.directory:./saree-images}") private String directory;
  public SareeImageIndexer(SareeRepository repository, ColorDetectionService detector) { this.repository = repository; this.detector = detector; }
  @Transactional public int reindex() throws IOException {
    Path root = Path.of(directory).toAbsolutePath().normalize();
    if (!Files.isDirectory(root)) throw new IllegalArgumentException("Image directory does not exist: " + root);
    try (Stream<Path> files = Files.walk(root)) {
      return (int) files.filter(Files::isRegularFile).filter(this::isImage).map(path -> indexOne(root, path)).filter(Objects::nonNull).count();
    }
  }
  @Transactional public Saree indexUploaded(Path file) throws IOException {
    Path root = Path.of(directory).toAbsolutePath().normalize();
    if (!file.toAbsolutePath().normalize().startsWith(root)) throw new IllegalArgumentException("Upload path is outside the image collection.");
    Saree result=indexOne(root,file); if(result==null) throw new IllegalArgumentException("The uploaded file could not be indexed."); return result;
  }
  private Saree indexOne(Path root, Path path) {
    try {
      VisualAnalysis analysis = detector.analyse(path); List<ColorInfo> colors = analysis.colors(); if (colors.isEmpty()) return null;
      String key = root.relativize(path).toString().replace('\\', '/');
      Saree s = repository.findByImageName(key).orElseGet(Saree::new);
      s.setImageName(key); s.setImagePath("/saree-images/" + key.replace(" ", "%20"));
      s.setPrimaryColor(colors.get(0).color()); s.setPrimaryColorPercentage(colors.get(0).percentage());
      if (colors.size() > 1) { s.setSecondaryColor(colors.get(1).color()); s.setSecondaryColorPercentage(colors.get(1).percentage()); }
      else { s.setSecondaryColor(null); s.setSecondaryColorPercentage(0); }
      s.setDesignSignature(Arrays.stream(analysis.signature()).mapToObj(Double::toString).collect(java.util.stream.Collectors.joining(",")));
      // Folder names supply safe starter metadata; admins can refine it through the API/UI.
      if (s.getSareeType()==null) s.setSareeType("Unclassified");
      if (s.getFabric()==null) s.setFabric("Unknown");
      if (s.getPattern()==null) s.setPattern(analysis.patternHint());
      if (s.getOccasion()==null) s.setOccasion("All occasions");
      return repository.save(s);
    } catch (RuntimeException e) { return null; } // bad images do not stop a full rebuild
  }
  private boolean isImage(Path p) { String n=p.getFileName().toString().toLowerCase(); return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png"); }
}
