package com.mip.importjob.dto;

import java.time.Instant;
import java.util.List;

public record ImportJobResponse(
        Long id,
        Long plantId,
        Long sourceDocumentId,
        String filename,
        String status,
        int totalRows,
        int autoImportedCount,
        int needsValidationCount,
        int rejectedCount,
        int invalidCount,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        List<ImportStepResponse> steps
) {
}
