package com.store.app.exception;

/**
 * Thrown when a password-reset token is missing, wrong, expired,
 * or already used. Mapped to HTTP 400.
 */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException(String message) {
        super(message);
    }
}
