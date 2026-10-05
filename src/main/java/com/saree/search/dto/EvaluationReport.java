package com.saree.search.dto;
public record EvaluationReport(int indexedSarees, int labelledSarees, int withDesignSignature,
                               double metadataCoverage, String note) {}
