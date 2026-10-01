package com.saree.search.dto;
import java.util.List;
public record SareeMatchResponse(boolean matched, int totalMatches, String message, List<ColorInfo> detectedColors, List<SareeMatch> matches) {}
