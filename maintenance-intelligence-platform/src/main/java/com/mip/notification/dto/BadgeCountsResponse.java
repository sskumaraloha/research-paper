package com.mip.notification.dto;

public record BadgeCountsResponse(
        long pendingValidation,
        long unreadNotifications,
        long insights
) {
}
