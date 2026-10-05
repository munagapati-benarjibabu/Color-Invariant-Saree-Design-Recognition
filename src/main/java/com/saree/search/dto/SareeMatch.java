package com.saree.search.dto;
public record SareeMatch(Long id, String imageName, String imagePath, double similarity, String primaryColor,
                         String sareeType, String fabric, String pattern, String occasion, Double price,
                         boolean inStock, String explanation) {}
