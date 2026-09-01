package com.store.app.exception;

/**
 * Thrown when OTP usage limits are exceeded: resend requested too soon,
 * too many OTPs issued in a time window, or too many verification attempts.
 * Mapped to HTTP 429.
 */
public class OtpRateLimitException extends RuntimeException {

    public OtpRateLimitException(String message) {
        super(message);
    }
}
