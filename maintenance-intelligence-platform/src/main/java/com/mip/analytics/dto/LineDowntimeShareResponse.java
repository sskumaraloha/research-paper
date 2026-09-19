package com.mip.analytics.dto;

public record LineDowntimeShareResponse(
        Long lineId,
        String lineName,
        long recordCount,
        long downtimeMinutes,
        double sharePct
) {
}
