package com.mip.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
        /** Page the emailed link opens; the raw token is appended as ?token=... */
        String url,
        int expiryMinutes,
        /** Cap on reset emails per account per hour. */
        int maxRequestsPerHour
) {
}
