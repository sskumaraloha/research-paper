package com.store.app.auth.dto;

import java.util.Set;

/**
 * Result of a successful customer registration. Contains no password.
 */
public record RegistrationResponse(
        Long id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        boolean phoneVerified,
        Set<String> roles,
        String message
) {
}
