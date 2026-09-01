package com.store.app.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Starts the forgot-password flow: requests an OTP for a registered phone.
 * Mutable class because it also backs the Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
public class ForgotPasswordRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;
}
