package com.mip.machine.dto;

import java.time.LocalDate;

public record MachineRowResponse(
        Long id,
        String code,
        String name,
        String lineName,
        String criticality,
        String status,
        long recordCount,
        long totalDowntimeMinutes,
        LocalDate lastMaintenanceDate
) {
}
