package com.saree.search.dto;

/** Optional catalogue filters; blank values mean "any". */
public record SearchFilters(String sareeType, String fabric, String pattern, String occasion,
                            Double maxPrice, Boolean inStockOnly, String colourMode) {}
