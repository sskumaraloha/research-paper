package com.store.app.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Verifies the forgot-password OTP. On success the caller receives a
 * short-lived reset token required by the actual password reset.
 * Mutable class because it also backs the Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
public class VerifyResetOtpRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits")
    private String otpCode;
}
