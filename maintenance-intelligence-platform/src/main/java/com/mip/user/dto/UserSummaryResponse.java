package com.mip.user.dto;

import java.util.List;

public record UserSummaryResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        String role,
        boolean active,
        List<Long> plantIds
) {
}
