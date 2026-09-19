package com.mip.part.dto;

import java.time.LocalDate;

public record PartRowResponse(
        Long id,
        String partNumber,
        String name,
        String category,
        long usageCount,
        LocalDate lastUsedDate
) {
}
