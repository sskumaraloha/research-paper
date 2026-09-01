package com.store.app.auth.dto;

import java.time.LocalDateTime;

/**
 * Issued after successful FORGOT_PASSWORD OTP verification. The token
 * is shown exactly once; only its hash is stored server-side.
 */
public record ResetTokenResponse(
        String message,
        String resetToken,
        LocalDateTime expiresAt
) {
}
