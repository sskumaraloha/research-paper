package com.mip.record.dto;

import com.mip.common.dto.NamedRef;

import java.time.LocalDate;
import java.util.List;

public record RecordDetailResponse(
        Long id,
        Long plantId,
        LocalDate recordDate,
        Long machineId,
        String machineCode,
        String machineName,
        String lineName,
        Long failureModeId,
        String failureMode,
        String failureModeCategory,
        int downtimeMinutes,
        String description,
        String actionTaken,
        String technician,
        String source,
        String status,
        Double confidence,
        String rejectedReason,
        List<NamedRef> spareParts,
        Long sourceDocumentId,
        String sourceDocumentName,
        String createdByName
) {
}
