package com.mip.analytics.dto;

public record MachineDowntimeResponse(
        Long machineId,
        String machineCode,
        String machineName,
        long recordCount,
        long downtimeMinutes
) {
}
