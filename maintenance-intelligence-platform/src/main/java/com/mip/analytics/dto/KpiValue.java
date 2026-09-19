package com.mip.analytics.dto;

public record KpiValue(
        String key,
        String label,
        double value,
        Double previousValue,
        /** Percent change vs the previous window; null when the previous window is empty. */
        Double changePct
) {
}
