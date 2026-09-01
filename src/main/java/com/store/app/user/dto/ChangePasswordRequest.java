package com.store.app.user.dto;

import com.store.app.auth.validation.PasswordConfirmable;
import com.store.app.auth.validation.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Self-service password change: requires the current password.
 * Mutable class because it also backs the profile Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
@PasswordMatches
public class ChangePasswordRequest implements PasswordConfirmable {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

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
