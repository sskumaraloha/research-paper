package com.store.app.exception;

/**
 * Thrown when a password and its confirmation do not match. Mapped to HTTP 400.
 */
public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException(String message) {
        super(message);
    }
}
