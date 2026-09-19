package com.mip.entry.dto;

import java.time.LocalDate;
import java.util.List;

public record EntryDraftResponse(
        Long machineId,
        String machineName,
        String machineText,
        LocalDate recordDate,
        Integer downtimeMinutes,
        String description,
        String actionTaken,
        Long failureModeId,
        String failureMode,
        String partsText,
        List<String> missingFields
) {
}
