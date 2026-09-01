package com.store.app.common.controller;

import com.store.app.common.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple health endpoint used to verify that the application is running.
 */
@RestController
public class HealthController {

    private static final String APPLICATION_NAME = "Store Management System";

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", APPLICATION_NAME);
    }
}
