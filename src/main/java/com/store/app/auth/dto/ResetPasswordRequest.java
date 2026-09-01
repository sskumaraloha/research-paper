package com.store.app.auth.dto;

import com.store.app.auth.validation.PasswordConfirmable;
import com.store.app.auth.validation.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Final step of the forgot-password flow. The reset token proves that
 * the caller completed OTP verification for this phone number.
 * Mutable class because it also backs the Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
@PasswordMatches
public class ResetPasswordRequest implements PasswordConfirmable {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, "
                    + "one digit and one special character")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    /** The {@link PasswordConfirmable} contract compares this with confirmPassword. */
    @Override
    public String getPassword() {
        return newPassword;
    }
}
