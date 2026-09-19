package com.mip.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String entityType,
        Long entityId,
        boolean read,
        Instant createdAt
) {
}
