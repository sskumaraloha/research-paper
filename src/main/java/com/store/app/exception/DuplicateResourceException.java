package com.store.app.exception;

/**
 * Thrown when creating or updating a resource would violate a
 * uniqueness constraint (e.g. duplicate email or phone number).
 * Mapped to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
