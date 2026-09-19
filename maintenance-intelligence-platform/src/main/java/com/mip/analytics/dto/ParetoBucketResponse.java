package com.mip.analytics.dto;

public record ParetoBucketResponse(
        Long failureModeId,
        String failureMode,
        String category,
        long recordCount,
        long downtimeMinutes,
        double downtimeSharePct,
        double cumulativeSharePct
) {
}
