package com.mip.user.dto;

import java.util.List;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String role,
        List<Long> plantIds
) {
}
