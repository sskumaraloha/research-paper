package com.mip.importjob.dto;

public record ImportSummaryResponse(
        Long jobId,
        String status,
        int totalRows,
        int autoImportedCount,
        int needsValidationCount,
        int rejectedCount,
        int invalidCount
) {
}
