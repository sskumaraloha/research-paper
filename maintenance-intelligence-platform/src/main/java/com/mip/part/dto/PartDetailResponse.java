package com.mip.part.dto;

import com.mip.common.dto.NamedRef;
import com.mip.record.dto.RecordRowResponse;

import java.time.LocalDate;
import java.util.List;

public record PartDetailResponse(
        Long id,
        String partNumber,
        String name,
        String category,
        long usageCount,
        LocalDate lastUsedDate,
        /** Average days between consecutive replacements; null with fewer than two usages. */
        Double avgReplacementIntervalDays,
        List<NamedRef> machinesUsedOn,
        List<RecordRowResponse> recentRecords
) {
}
