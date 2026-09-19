package com.mip.user.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserProfileResponse user
) {
}
