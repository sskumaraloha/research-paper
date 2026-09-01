package com.store.app.user.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Public representation of a user.
 * <p>
 * Deliberately contains no password field, so the password hash
 * can never be serialized into an API response.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        boolean enabled,
        boolean phoneVerified,
        Set<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
