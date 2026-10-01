package com.saree.search.service;

import com.saree.search.dto.ColorInfo;
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
  private Saree indexOne(Path root, Path path) {
    try {
      List<ColorInfo> colors = detector.detect(path); if (colors.isEmpty()) return null;
      String key = root.relativize(path).toString().replace('\\', '/');
      Saree s = repository.findByImageName(key).orElseGet(Saree::new);
      s.setImageName(key); s.setImagePath("/saree-images/" + key.replace(" ", "%20"));
      s.setPrimaryColor(colors.get(0).color()); s.setPrimaryColorPercentage(colors.get(0).percentage());
      if (colors.size() > 1) { s.setSecondaryColor(colors.get(1).color()); s.setSecondaryColorPercentage(colors.get(1).percentage()); }
      else { s.setSecondaryColor(null); s.setSecondaryColorPercentage(0); }
      return repository.save(s);
    } catch (RuntimeException e) { return null; } // bad images do not stop a full rebuild
  }
  private boolean isImage(Path p) { String n=p.getFileName().toString().toLowerCase(); return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png"); }
}
