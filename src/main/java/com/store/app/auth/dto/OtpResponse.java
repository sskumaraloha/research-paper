package com.store.app.auth.dto;

import java.time.LocalDateTime;

/**
 * Result of an OTP send or verify operation.
 *
 * @param expiresAt when the sent OTP expires; {@code null} for verify results
 */
public record OtpResponse(String message, LocalDateTime expiresAt) {

    public static OtpResponse of(String message) {
        return new OtpResponse(message, null);
    }
}
