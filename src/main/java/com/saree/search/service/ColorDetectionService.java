package com.saree.search.service;

import com.saree.search.dto.ColorInfo;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacpp.BytePointer;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.*;

@Service
public class ColorDetectionService {
  public List<ColorInfo> detect(Path file) {
    Mat source = imread(file.toString(), IMREAD_COLOR);
    if (source.empty()) throw new IllegalArgumentException("The uploaded file is not a readable image.");
    Mat resized = new Mat(); Mat hsv = new Mat();
    resize(source, resized, new org.bytedeco.opencv.opencv_core.Size(240, 240));
    cvtColor(resized, hsv, COLOR_BGR2HSV);
    Map<String, Integer> counts = new HashMap<>(); int usable = 0;
    for (int y = 0; y < hsv.rows(); y += 2) for (int x = 0; x < hsv.cols(); x += 2) {
      BytePointer pixel = hsv.ptr(y, x);
      int h = pixel.get(0) & 0xff, s = pixel.get(1) & 0xff, v = pixel.get(2) & 0xff;
      String name = classify(h, s, v); if (name != null) { counts.merge(name, 1, Integer::sum); usable++; }
    }
    source.release(); resized.release(); hsv.release();
    if (usable == 0) throw new IllegalArgumentException("No usable colour pixels were found in this image.");
    final int total = usable;
    return counts.entrySet().stream().map(e -> new ColorInfo(e.getKey(), round(e.getValue() * 100.0 / total)))
      .filter(c -> c.percentage() >= 3).sorted(Comparator.comparingDouble(ColorInfo::percentage).reversed()).limit(3).toList();
  }
  private String classify(int h, int s, int v) {
    if (v < 45) return "BLACK";
    if (s < 24) return v > 215 ? "WHITE" : v > 160 ? "SILVER" : "GREY";
    if (v > 190 && s < 70) return "CREAM";
    if (h >= 8 && h < 23 && v < 120) return "BROWN";
    if (h >= 8 && h < 23 && s < 145 && v > 130) return "GOLD";
    if (h >= 8 && h < 24 && v > 105) return s < 105 ? "BEIGE" : "ORANGE";
    if (h >= 24 && h < 38) return "YELLOW";
    if (h >= 38 && h < 82) return h > 58 && v < 120 ? "DARK_GREEN" : "GREEN";
    if (h >= 82 && h < 110) return "BLUE";
    if (h >= 110 && h < 132) return "NAVY_BLUE";
    if (h >= 132 && h < 150) return "PURPLE";
    if (h >= 150 && h < 170) return "VIOLET";
    if (h >= 165 || h < 8) return v < 115 ? "MAROON" : (v > 165 && s < 175 ? "PINK" : "RED");
    return "BROWN";
  }
  private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}
