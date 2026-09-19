package com.mip.analytics.dto;

public record FailureModeStatsResponse(
        Long failureModeId,
        String name,
        String category,
        long recordCount,
        long totalDowntimeMinutes,
        double avgDowntimeMinutes,
        long machinesAffected
) {
}
