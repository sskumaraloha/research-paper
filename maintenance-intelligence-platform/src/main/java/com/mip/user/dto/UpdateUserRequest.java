package com.mip.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** All fields optional; only supplied fields change. */
public record UpdateUserRequest(
        @Size(max = 100) String fullName,
        String role,
        @Pattern(regexp = "\\+?[0-9]{8,15}", message = "must be 8-15 digits, optionally with +")
        String phoneNumber,
        List<Long> plantIds,
        Boolean active
) {
}
