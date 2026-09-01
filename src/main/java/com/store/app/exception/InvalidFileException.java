package com.store.app.exception;

/**
 * Thrown when an uploaded file fails validation (empty, too large,
 * or unsupported type). Mapped to HTTP 400.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
