package com.saree.search.dto;
import java.util.List;
public record VisualAnalysis(List<ColorInfo> colors, double[] signature, double foregroundCoverage,
                             String patternHint) {}
