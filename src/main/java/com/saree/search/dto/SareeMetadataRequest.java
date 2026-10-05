package com.saree.search.dto;
import jakarta.validation.constraints.NotBlank;
public record SareeMetadataRequest(@NotBlank String sareeType, @NotBlank String fabric, @NotBlank String pattern,
                                   String occasion, Double price, Boolean inStock) {}
