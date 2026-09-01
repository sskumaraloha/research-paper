package com.store.app.auth.service.impl;

import com.store.app.auth.config.OtpProperties;
import com.store.app.auth.dto.ForgotPasswordRequest;
import com.store.app.auth.dto.OtpResponse;
import com.store.app.auth.dto.ResetPasswordRequest;
import com.store.app.auth.dto.ResetTokenResponse;
import com.store.app.auth.dto.SendOtpRequest;
import com.store.app.auth.dto.VerifyOtpRequest;
import com.store.app.auth.dto.VerifyResetOtpRequest;
import com.store.app.auth.entity.OtpPurpose;
import com.store.app.auth.entity.PasswordResetToken;
import com.store.app.auth.repository.PasswordResetTokenRepository;
import com.store.app.auth.service.OtpService;
import com.store.app.auth.service.PasswordResetService;
import com.store.app.exception.InvalidResetTokenException;
import com.store.app.exception.PasswordMismatchException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.user.entity.User;
import com.store.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final OtpService otpService;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;

    @Override
    public OtpResponse sendOtp(ForgotPasswordRequest request) {
        return otpService.sendOtp(new SendOtpRequest(
                request.getPhoneNumber(), OtpPurpose.FORGOT_PASSWORD));
    }

    /**
     * Not {@code @Transactional}: the underlying OTP verification persists
     * failed-attempt counters even when it throws, which a surrounding
     * transaction would roll back.
     */
    @Override
    public ResetTokenResponse verifyOtp(VerifyResetOtpRequest request) {
        otpService.verifyOtp(new VerifyOtpRequest(
                request.getPhoneNumber(), request.getOtpCode(), OtpPurpose.FORGOT_PASSWORD));

        // One live reset token per phone number. Explicit saveAll because this
        // method runs outside a transaction (no dirty-checking flush).
        var staleTokens = tokenRepository.findAllByPhoneNumberAndUsedFalse(
                request.getPhoneNumber());
        staleTokens.forEach(PasswordResetToken::markUsed);
        tokenRepository.saveAll(staleTokens);

        String plainToken = generateToken();
        LocalDateTime expiresAt =
                LocalDateTime.now().plusMinutes(otpProperties.resetTokenExpiryMinutes());
        tokenRepository.save(new PasswordResetToken(
                request.getPhoneNumber(), passwordEncoder.encode(plainToken), expiresAt));

        return new ResetTokenResponse(
                "OTP verified. Use the reset token to set a new password within "
                        + otpProperties.resetTokenExpiryMinutes() + " minutes.",
                plainToken,
                expiresAt);
    }

    @Override
    @Transactional
    public OtpResponse resetPassword(ResetPasswordRequest request) {
        // Defense in depth alongside the @PasswordMatches constraint.
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Password and confirm password do not match");
        }

        PasswordResetToken token = tokenRepository
                .findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(request.getPhoneNumber())
                .orElseThrow(() -> new InvalidResetTokenException(
                        "No active password reset request found. Please start over."));

        if (token.isExpired()) {
            throw new InvalidResetTokenException(
                    "The reset token has expired. Please start over.");
        }
        if (!passwordEncoder.matches(request.getResetToken(), token.getTokenHash())) {
            throw new InvalidResetTokenException("Invalid reset token. Please start over.");
        }

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with phone number: " + request.getPhoneNumber()));

        token.markUsed();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        tokenRepository.save(token);

        log.info("Password reset completed for user id {}", user.getId());
        return OtpResponse.of(
                "Password reset successfully. Please log in with your new password.");
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
