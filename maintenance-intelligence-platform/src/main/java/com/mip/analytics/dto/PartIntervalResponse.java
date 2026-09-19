package com.mip.analytics.dto;

public record PartIntervalResponse(
        Long partId,
        String partName,
        long usageCount,
        /** Average days between consecutive replacements; null with fewer than two usages. */
        Double avgIntervalDays
) {
}
