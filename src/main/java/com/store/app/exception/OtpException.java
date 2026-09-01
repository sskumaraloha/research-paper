package com.store.app.exception;

/**
 * Thrown when an OTP cannot be sent or verified for a business reason
 * (no active OTP, expired, wrong code, phone already verified).
 * Mapped to HTTP 400.
 */
public class OtpException extends RuntimeException {

    public OtpException(String message) {
        super(message);
    }
}
