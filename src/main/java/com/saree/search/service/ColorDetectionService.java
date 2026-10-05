package com.saree.search.service;

import com.saree.search.dto.ColorInfo;
import com.saree.search.dto.VisualAnalysis;
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
  public List<ColorInfo> detect(Path file) { return analyse(file).colors(); }
  /** Foreground-biased, colour-invariant texture descriptor. No uploaded image leaves this server. */
  public VisualAnalysis analyse(Path file) {
    Mat source = imread(file.toString(), IMREAD_COLOR);
    if (source.empty()) throw new IllegalArgumentException("The uploaded file is not a readable image.");
    Mat resized = new Mat(); Mat hsv = new Mat();
    resize(source, resized, new org.bytedeco.opencv.opencv_core.Size(240, 240));
    cvtColor(resized, hsv, COLOR_BGR2HSV);
    Map<String, Integer> counts = new HashMap<>(); int usable = 0, eligible = 0;
    double[] grid = new double[4], orientation = new double[4];
    for (int y = 3; y < hsv.rows() - 3; y += 2) for (int x = 3; x < hsv.cols() - 3; x += 2) {
      // Product-photo background is most often around the outer boundary.
      if (x < 19 || x > 220 || y < 19 || y > 220) continue;
      BytePointer pixel = hsv.ptr(y, x);
      int h = pixel.get(0) & 0xff, s = pixel.get(1) & 0xff, v = pixel.get(2) & 0xff;
      eligible++;
      // Exclude neutral white only at the crop edge; a central white saree stays searchable.
      if (v > 246 && s < 12 && (x < 45 || x > 195 || y < 45 || y > 195)) continue;
      String name = classify(h, s, v); counts.merge(name, 1, Integer::sum); usable++;
      int left=hsv.ptr(y,x-3).get(2)&255, right=hsv.ptr(y,x+3).get(2)&255;
      int up=hsv.ptr(y-3,x).get(2)&255, down=hsv.ptr(y+3,x).get(2)&255;
      double dx=right-left, dy=down-up, magnitude=Math.min(255,Math.hypot(dx,dy))/255.0;
      grid[(y < 120 ? 0 : 2) + (x < 120 ? 0 : 1)] += magnitude;
      if(magnitude>.10) { double angle=(Math.atan2(dy,dx)+Math.PI)%Math.PI; orientation[Math.min(3,(int)(angle/(Math.PI/4)))] += magnitude; }
    }
    source.release(); resized.release(); hsv.release();
    if (usable == 0) throw new IllegalArgumentException("No usable colour pixels were found in this image.");
    final int total = usable;
    List<ColorInfo> colors = counts.entrySet().stream().map(e -> new ColorInfo(e.getKey(), round(e.getValue() * 100.0 / total)))
      .filter(c -> c.percentage() >= 3).sorted(Comparator.comparingDouble(ColorInfo::percentage).reversed()).limit(3).toList();
    double[] signature = new double[8]; double norm=Math.max(1,usable);
    for(int i=0;i<4;i++){ signature[i]=grid[i]/norm; signature[i+4]=orientation[i]/norm; }
    double length=Math.sqrt(Arrays.stream(signature).map(v->v*v).sum()); if(length>0) for(int i=0;i<signature.length;i++) signature[i]/=length;
    double texture=Arrays.stream(grid).sum()/norm;
    return new VisualAnalysis(colors,signature,round(usable*100.0/Math.max(1,eligible)),texture>.32?"detailed / textured":texture>.18?"patterned":"minimal / smooth");
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
