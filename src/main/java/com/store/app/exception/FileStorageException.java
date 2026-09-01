package com.store.app.exception;

/**
 * Thrown when a file cannot be stored or read due to an I/O problem.
 * Mapped to HTTP 500.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }
}
