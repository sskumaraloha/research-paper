package com.store.app.auth.dto;

import com.store.app.auth.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to verify a previously sent OTP.
 * <p>
 * Mutable class (not a record) because it also serves as the
 * Thymeleaf form-backing bean on the verification page.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits")
    private String otpCode;

    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;
}
