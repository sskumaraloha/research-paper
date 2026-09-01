package com.store.app.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error body returned by the global exception handler.
 *
 * @param fieldErrors per-field validation messages; {@code null} unless the
 *                    error was caused by request validation
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), null);
    }

    public static ErrorResponse ofValidation(int status, String error, String message,
                                             String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), fieldErrors);
    }
}
