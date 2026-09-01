package com.store.app;

import com.store.app.auth.config.OtpProperties;
import com.store.app.auth.dto.LoginRequest;
import com.store.app.auth.dto.LoginResponse;
import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.dto.SendOtpRequest;
import com.store.app.auth.dto.VerifyOtpRequest;
import com.store.app.auth.entity.OtpPurpose;
import com.store.app.auth.repository.OtpRepository;
import com.store.app.auth.service.AuthService;
import com.store.app.auth.service.OtpService;
import com.store.app.auth.service.impl.AbstractOtpService;
import com.store.app.exception.AuthenticationFailedException;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.OtpException;
import com.store.app.exception.OtpRateLimitException;
import com.store.app.security.jwt.JwtService;
import com.store.app.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end authentication and OTP security: registration, phone
 * verification (with the real hashed-OTP verify path via a capturing
 * delivery hook), login gating, JWT claims and tamper rejection,
 * wrong-code attempt counting, single use, and resend throttling.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthOtpFlowTest {

    /** Captures the plain OTP at delivery so tests can use the real verify path. */
    @TestConfiguration
    static class CapturingOtpConfig {

        static final AtomicReference<String> LAST_OTP = new AtomicReference<>();

        @Bean
        @Primary
        OtpService capturingOtpService(OtpRepository otpRepository,
                                       UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       OtpProperties properties) {
            return new AbstractOtpService(
                    otpRepository, userRepository, passwordEncoder, properties) {
                @Override
                protected void deliverOtp(String phoneNumber, String otpCode) {
                    LAST_OTP.set(otpCode);
                }
            };
        }
    }

    @Autowired
    private AuthService authService;
    @Autowired
    private OtpService otpService;
    @Autowired
    private JwtService jwtService;

    private String phone;
    private String email;

    @BeforeEach
    void register() {
        long nonce = System.nanoTime();
        phone = String.valueOf(9300000000L + (nonce % 100000000L));
        email = "auth" + nonce + "@example.com";

        RegistrationRequest registration = new RegistrationRequest();
        registration.setFirstName("Auth");
        registration.setLastName("Tester");
        registration.setPhoneNumber(phone);
        registration.setEmail(email);
        registration.setPassword("Secret@123");
        registration.setConfirmPassword("Secret@123");
        authService.registerCustomer(registration);
    }

    @Test
    void duplicateRegistrationIsRejected() {
        RegistrationRequest duplicate = new RegistrationRequest();
        duplicate.setFirstName("Auth");
        duplicate.setLastName("Tester");
        duplicate.setPhoneNumber(phone);
        duplicate.setEmail("other-" + email);
        duplicate.setPassword("Secret@123");
        duplicate.setConfirmPassword("Secret@123");

        assertThatThrownBy(() -> authService.registerCustomer(duplicate))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void fullFlowVerifyThenLoginWithValidJwt() {
        // Unverified customers cannot log in.
        assertThatThrownBy(() -> authService.login(new LoginRequest(phone, "Secret@123")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("not verified");

        // Real OTP round trip using the captured plain code.
        otpService.sendOtp(new SendOtpRequest(phone, OtpPurpose.REGISTRATION));
        String code = CapturingOtpConfig.LAST_OTP.get();
        assertThat(code).matches("\\d{6}");
        otpService.verifyOtp(new VerifyOtpRequest(phone, code, OtpPurpose.REGISTRATION));

        // Wrong password still rejected; right password issues a JWT.
        assertThatThrownBy(() -> authService.login(new LoginRequest(phone, "Wrong@123")))
                .isInstanceOf(AuthenticationFailedException.class);

        LoginResponse login = authService.login(new LoginRequest(phone, "Secret@123"));
        assertThat(login.tokenType()).isEqualTo("Bearer");
        assertThat(login.user().roles()).containsExactly("ROLE_CUSTOMER");

        // JWT carries the promised claims and rejects tampering.
        Claims claims = jwtService.parseClaims(login.accessToken());
        assertThat(claims.getSubject()).isEqualTo(phone);
        assertThat(claims.get("userId", Long.class)).isEqualTo(login.user().id());
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_CUSTOMER");

        String tampered = login.accessToken().substring(0, login.accessToken().length() - 3) + "abc";
        assertThatThrownBy(() -> jwtService.parseClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void wrongCodesCountAttemptsAndOtpIsSingleUse() {
        otpService.sendOtp(new SendOtpRequest(phone, OtpPurpose.REGISTRATION));
        String code = CapturingOtpConfig.LAST_OTP.get();
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        assertThatThrownBy(() -> otpService.verifyOtp(
                new VerifyOtpRequest(phone, wrongCode, OtpPurpose.REGISTRATION)))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("attempt(s) remaining");

        // The right code still works after failed attempts...
        otpService.verifyOtp(new VerifyOtpRequest(phone, code, OtpPurpose.REGISTRATION));

        // ...but only once: a verified OTP can never be replayed.
        assertThatThrownBy(() -> otpService.verifyOtp(
                new VerifyOtpRequest(phone, code, OtpPurpose.REGISTRATION)))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("No active OTP");
    }

    @Test
    void resendIsThrottled() {
        otpService.sendOtp(new SendOtpRequest(phone, OtpPurpose.REGISTRATION));

        assertThatThrownBy(() -> otpService.sendOtp(
                new SendOtpRequest(phone, OtpPurpose.REGISTRATION)))
                .isInstanceOf(OtpRateLimitException.class)
                .hasMessageContaining("wait");
    }
}
