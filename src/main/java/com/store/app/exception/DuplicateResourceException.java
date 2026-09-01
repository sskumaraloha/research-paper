package com.store.app.exception;

import lombok.Getter;

/**
 * Thrown when creating or updating a resource would violate a
 * uniqueness constraint (e.g. duplicate email or phone number).
 * Mapped to HTTP 409.
 */
@Getter
public class DuplicateResourceException extends RuntimeException {

    /** Name of the offending field (e.g. "email"), or {@code null} if not field-specific. */
    private final String field;

    public DuplicateResourceException(String message) {
        this(null, message);
    }

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }
}
