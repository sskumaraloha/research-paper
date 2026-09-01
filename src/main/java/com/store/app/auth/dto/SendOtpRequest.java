package com.store.app.auth.dto;

import com.store.app.auth.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request to send (or resend) an OTP to a phone number.
 */
public record SendOtpRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
        String phoneNumber,

        @NotNull(message = "Purpose is required")
        OtpPurpose purpose
) {
}
