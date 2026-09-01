package com.store.app.exception;

/**
 * Thrown when an operation is valid in form but forbidden by business
 * rules in the current state (e.g. deleting a category that still has
 * subcategories). Mapped to HTTP 409.
 */
public class OperationNotAllowedException extends RuntimeException {

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
