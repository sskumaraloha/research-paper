package com.store.app.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Self-service profile update: name and email only. The phone number is
 * verified by OTP and stays read-only here, and there is deliberately
 * no role field — customers can never change their own role.
 * Mutable class because it also backs the profile Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]*$",
            message = "First name may contain only letters, spaces, dots, apostrophes and hyphens")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]*$",
            message = "Last name may contain only letters, spaces, dots, apostrophes and hyphens")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
}
