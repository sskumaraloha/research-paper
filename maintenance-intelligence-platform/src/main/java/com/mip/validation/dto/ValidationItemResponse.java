package com.mip.validation.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ValidationItemResponse(
        Long id,
        Long jobId,
        String sourceFilename,
        int rowNumber,
        List<String> reasons,
        Double confidence,
        String machineText,
        Long machineId,
        String machineName,
        Long failureModeId,
        String failureModeName,
        LocalDate recordDate,
        Integer downtimeMinutes,
        String description,
        String actionTaken,
        String technician,
        String partsText,
        Map<String, String> rawData
) {
}
