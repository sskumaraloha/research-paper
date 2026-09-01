package com.store.app.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT settings bound from the {@code app.jwt.*} configuration keys.
 *
 * @param secret            base64-encoded HMAC signing key (>= 256 bits);
 *                          profile-specific, from the environment in prod
 * @param expirationMinutes access-token lifetime
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("60") long expirationMinutes
) {
}
