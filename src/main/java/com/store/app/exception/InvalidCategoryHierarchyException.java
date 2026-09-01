package com.store.app.exception;

/**
 * Thrown when a category update would create an invalid hierarchy,
 * e.g. a category becoming its own parent or a descendant of itself.
 * Mapped to HTTP 400.
 */
public class InvalidCategoryHierarchyException extends RuntimeException {

    public InvalidCategoryHierarchyException(String message) {
        super(message);
    }
}
