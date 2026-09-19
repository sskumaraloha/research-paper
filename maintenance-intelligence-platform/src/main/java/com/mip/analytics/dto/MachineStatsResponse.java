package com.mip.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record MachineStatsResponse(
        Long machineId,
        String machineCode,
        String machineName,
        String status,
        long recordCount,
        long totalDowntimeMinutes,
        double avgDowntimeMinutes,
        /** Mean days between failures; null with fewer than two records. */
        Double mtbfDays,
        LocalDate lastMaintenanceDate,
        List<FailureModeStatsResponse> topFailureModes
) {
}
