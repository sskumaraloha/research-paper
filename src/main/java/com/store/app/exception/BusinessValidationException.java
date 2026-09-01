package com.store.app.exception;

/**
 * Thrown when a request is well-formed but violates a business rule
 * (e.g. discount price not lower than the price). Mapped to HTTP 400.
 */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
