package com.mip.record.dto;

import java.time.LocalDate;

public record RecordRowResponse(
        Long id,
        LocalDate recordDate,
        Long machineId,
        String machineCode,
        String machineName,
        String failureMode,
        String failureModeCategory,
        int downtimeMinutes,
        String technician,
        String source,
        String status,
        Double confidence,
        String description
) {
}
