package com.mip.user.dto;

public record UserSummaryResponse(
        Long id,
        String fullName,
        String email,
        String role,
        boolean active
) {
}
