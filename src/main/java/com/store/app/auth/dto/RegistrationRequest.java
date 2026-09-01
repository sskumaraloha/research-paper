package com.store.app.auth.dto;

import com.store.app.auth.validation.PasswordConfirmable;
import com.store.app.auth.validation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer registration input.
 * <p>
 * Mutable class (not a record) because it also serves as the
 * Thymeleaf form-backing bean. Deliberately has no role field:
 * clients can never choose their own role.
 */
@Getter
@Setter
@NoArgsConstructor
@PasswordMatches
public class RegistrationRequest implements PasswordConfirmable {

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

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, "
                    + "one digit and one special character")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
