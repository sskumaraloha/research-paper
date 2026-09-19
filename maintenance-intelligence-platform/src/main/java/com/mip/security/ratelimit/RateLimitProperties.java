package com.mip.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        /** Requests per window for authenticated/general API traffic, keyed by user (or IP). */
        int generalLimit,
        int generalWindowSeconds,
        /** Stricter budget for credential endpoints and the webhook, keyed by IP. */
        int authLimit,
        int authWindowSeconds
) {
}
