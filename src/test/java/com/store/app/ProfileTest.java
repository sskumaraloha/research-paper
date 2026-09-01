package com.store.app;

import com.store.app.auth.dto.LoginRequest;
import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.service.AuthService;
import com.store.app.exception.AuthenticationFailedException;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.user.dto.ChangePasswordRequest;
import com.store.app.user.dto.UpdateProfileRequest;
import com.store.app.user.dto.UserResponse;
import com.store.app.user.repository.UserRepository;
import com.store.app.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies self-service profile updates and password change:
 * uniqueness, wrong-current-password rejection, JWT-relevant
 * password rotation, and that roles/phone can never change here.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProfileTest {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private String phone;
    private String email;

    @BeforeEach
    void seedUser() {
        long nonce = System.nanoTime();
        phone = String.valueOf(6500000000L + (nonce % 100000000L));
        email = "profile" + nonce + "@example.com";

        RegistrationRequest registration = new RegistrationRequest();
        registration.setFirstName("Pat");
        registration.setLastName("Profile");
        registration.setPhoneNumber(phone);
        registration.setEmail(email);
        registration.setPassword("Secret@123");
        registration.setConfirmPassword("Secret@123");
        userId = authService.registerCustomer(registration).id();

        // Verify the phone so login works after the password change test.
        markPhoneVerified();
    }

    @Test
    void profileUpdateChangesNameAndEmailButNeverRolesOrPhone() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Patricia");
        request.setLastName("Profiler");
        request.setEmail("new-" + email);

        UserResponse updated = userService.updateProfile(userId, request);

        assertThat(updated.firstName()).isEqualTo("Patricia");
        assertThat(updated.email()).isEqualTo("new-" + email);
        assertThat(updated.phoneNumber()).isEqualTo(phone);
        assertThat(updated.roles()).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void duplicateEmailIsRejected() {
        long nonce = System.nanoTime();
        RegistrationRequest other = new RegistrationRequest();
        other.setFirstName("Other");
        other.setLastName("User");
        other.setPhoneNumber(String.valueOf(6600000000L + (nonce % 100000000L)));
        other.setEmail("taken" + nonce + "@example.com");
        other.setPassword("Secret@123");
        other.setConfirmPassword("Secret@123");
        authService.registerCustomer(other);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Pat");
        request.setLastName("Profile");
        request.setEmail("taken" + nonce + "@example.com");

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() {
        ChangePasswordRequest wrong = passwordRequest("WrongOne@1", "Fresh@1234");
        assertThatThrownBy(() -> userService.changePassword(userId, wrong))
                .isInstanceOf(AuthenticationFailedException.class);

        userService.changePassword(userId, passwordRequest("Secret@123", "Fresh@1234"));

        // Old credentials stop working, new ones log in.
        assertThatThrownBy(() -> authService.login(new LoginRequest(phone, "Secret@123")))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(authService.login(new LoginRequest(phone, "Fresh@1234")).accessToken())
                .isNotBlank();

        // passwordChangedAt stamped -> pre-change JWTs are revoked.
        assertThat(userRepository.findById(userId).orElseThrow().getPasswordChangedAt())
                .isNotNull();
    }

    // ------------------------------------------------------------------

    private ChangePasswordRequest passwordRequest(String current, String next) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(next);
        request.setConfirmPassword(next);
        return request;
    }

    private void markPhoneVerified() {
        // Login requires a verified phone; flip the flag directly for the test.
        var user = userRepository.findById(userId).orElseThrow();
        user.setPhoneVerified(true);
        userRepository.save(user);
    }
}
