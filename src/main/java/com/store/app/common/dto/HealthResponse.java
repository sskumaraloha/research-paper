package com.store.app.common.dto;

/**
 * Response body for the application health endpoint.
 */
public record HealthResponse(String status, String application) {
}
