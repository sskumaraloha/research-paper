package com.mip.record.dto;

import java.time.Instant;

public record SourceDocumentResponse(
        Long id,
        String filename,
        String contentType,
        long sizeBytes,
        String uploadedByName,
        Instant uploadedAt
) {
}
