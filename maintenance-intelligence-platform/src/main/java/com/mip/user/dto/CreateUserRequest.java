package com.mip.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String role,
        @Pattern(regexp = "\\+?[0-9]{8,15}", message = "must be 8-15 digits, optionally with +")
        String phoneNumber,
        List<Long> plantIds
) {
}
