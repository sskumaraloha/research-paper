package com.mip.machine.dto;

import java.time.LocalDate;
import java.util.List;

public record MachineDetailResponse(
        Long id,
        Long plantId,
        String plantName,
        Long lineId,
        String lineName,
        String code,
        String name,
        String manufacturer,
        String model,
        String criticality,
        LocalDate commissionedOn,
        boolean active,
        String status,
        long recordCount,
        long totalDowntimeMinutes,
        LocalDate lastMaintenanceDate,
        List<AliasResponse> aliases
) {
}
