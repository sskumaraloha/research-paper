package com.mip.search.dto;

import java.time.LocalDate;

public record RecordMatchResponse(
        Long id,
        LocalDate recordDate,
        String machineName,
        String failureMode,
        int downtimeMinutes,
        String snippet
) {
}
