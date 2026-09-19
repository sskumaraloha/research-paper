package com.mip.analytics.dto;

import java.util.List;

public record TrendResponse(
        Long plantId,
        List<TrendPoint> points
) {
    public record TrendPoint(String yearMonth, long recordCount, long downtimeMinutes) {
    }
}
