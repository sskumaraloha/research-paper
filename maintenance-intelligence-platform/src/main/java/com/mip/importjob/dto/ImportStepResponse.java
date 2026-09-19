package com.mip.importjob.dto;

import java.time.Instant;

public record ImportStepResponse(
        String name,
        String status,
        int processedCount,
        String message,
        Instant startedAt,
        Instant finishedAt
) {
}
