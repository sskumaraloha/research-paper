package com.mip.audit.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorName,
        String action,
        String entityType,
        Long entityId,
        Long plantId,
        String detail,
        Instant occurredAt
) {
}
