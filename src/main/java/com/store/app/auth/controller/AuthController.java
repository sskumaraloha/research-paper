package com.store.app.auth.controller;

import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.dto.RegistrationResponse;
import com.store.app.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for authentication flows.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse response = authService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
