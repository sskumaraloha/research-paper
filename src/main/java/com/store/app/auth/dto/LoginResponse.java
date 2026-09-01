package com.store.app.auth.dto;

import com.store.app.user.dto.UserResponse;

/**
 * Successful API login result. {@link UserResponse} contains no
 * password or password hash by construction.
 *
 * @param expiresIn access-token lifetime in seconds
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public static LoginResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, user);
    }
}
