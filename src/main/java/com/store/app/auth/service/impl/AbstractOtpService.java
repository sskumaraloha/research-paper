package com.store.app.auth.service.impl;

import com.store.app.auth.config.OtpProperties;
import com.store.app.auth.dto.OtpResponse;
import com.store.app.auth.dto.SendOtpRequest;
import com.store.app.auth.dto.VerifyOtpRequest;
import com.store.app.auth.entity.Otp;
import com.store.app.auth.entity.OtpPurpose;
import com.store.app.auth.repository.OtpRepository;
import com.store.app.auth.service.OtpService;
import com.store.app.exception.OtpException;
import com.store.app.exception.OtpRateLimitException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.user.entity.User;
import com.store.app.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Template implementation of {@link OtpService} containing the complete
 * OTP lifecycle: generation, hashing, rate limiting, expiry, single-use
 * verification, and attempt counting.
 * <p>
 * Subclasses provide only the delivery channel by implementing
 * {@link #deliverOtp(String, String)} — e.g. a console logger for
 * development or an SMS gateway (Twilio, etc.) for production.
 */
public abstract class AbstractOtpService implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties properties;

    protected AbstractOtpService(OtpRepository otpRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 OtpProperties properties) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    /**
     * Delivers the plain OTP code to the user. Implementations must not
     * persist the plain code anywhere.
     */
    protected abstract void deliverOtp(String phoneNumber, String otpCode);

    @Override
    @Transactional
    public OtpResponse sendOtp(SendOtpRequest request) {
        String phoneNumber = request.phoneNumber();
        OtpPurpose purpose = request.purpose();

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with phone number: " + phoneNumber));

        if (purpose == OtpPurpose.REGISTRATION && user.isPhoneVerified()) {
            throw new OtpException("Phone number is already verified");
        }

        enforceRateLimits(phoneNumber, purpose);

        // Requirement: issuing a new OTP invalidates all previously active ones.
        LocalDateTime now = LocalDateTime.now();
        otpRepository.findAllByPhoneNumberAndPurposeAndVerifiedFalseAndExpiryTimeAfter(
                phoneNumber, purpose, now).forEach(Otp::invalidate);

        String otpCode = generateOtpCode();
        LocalDateTime expiryTime = now.plusMinutes(properties.expiryMinutes());
        otpRepository.save(new Otp(
                phoneNumber, passwordEncoder.encode(otpCode), purpose, expiryTime));

        deliverOtp(phoneNumber, otpCode);

        return new OtpResponse(
                "OTP sent to " + maskPhoneNumber(phoneNumber)
                        + ". It expires in " + properties.expiryMinutes() + " minutes.",
                expiryTime);
    }

    /**
     * Deliberately not {@code @Transactional}: a failed attempt both
     * increments the attempt counter and throws, and a transaction would
     * roll the increment back — letting attackers retry forever. Each
     * repository save commits on its own.
     */
    @Override
    public OtpResponse verifyOtp(VerifyOtpRequest request) {
        Otp otp = otpRepository
                .findTopByPhoneNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                        request.getPhoneNumber(), request.getPurpose())
                .orElseThrow(() -> new OtpException(
                        "No active OTP found. Please request a new one."));

        if (otp.isExpired()) {
            throw new OtpException("OTP has expired. Please request a new one.");
        }
        if (otp.getAttemptCount() >= properties.maxAttempts()) {
            throw new OtpRateLimitException(
                    "Maximum verification attempts exceeded. Please request a new OTP.");
        }

        otp.incrementAttemptCount();

        if (!passwordEncoder.matches(request.getOtpCode(), otp.getOtpCode())) {
            otpRepository.save(otp);
            int remaining = properties.maxAttempts() - otp.getAttemptCount();
            throw new OtpException("Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        // Single use: a verified OTP can never match the active-OTP query again.
        otp.markVerified();
        otpRepository.save(otp);

        if (request.getPurpose() == OtpPurpose.REGISTRATION) {
            User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No account found with phone number: " + request.getPhoneNumber()));
            user.setPhoneVerified(true);
            userRepository.save(user);
            return OtpResponse.of("Phone number verified successfully");
        }

        return OtpResponse.of("OTP verified successfully");
    }

    private void enforceRateLimits(String phoneNumber, OtpPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();

        long recentRequests = otpRepository.countByPhoneNumberAndPurposeAndCreatedAtAfter(
                phoneNumber, purpose, now.minusHours(1));
        if (recentRequests >= properties.maxRequestsPerHour()) {
            throw new OtpRateLimitException(
                    "Too many OTP requests for this phone number. Please try again later.");
        }

        otpRepository.findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(phoneNumber, purpose)
                .filter(last -> last.getCreatedAt()
                        .isAfter(now.minusSeconds(properties.resendCooldownSeconds())))
                .ifPresent(last -> {
                    throw new OtpRateLimitException(
                            "Please wait " + properties.resendCooldownSeconds()
                                    + " seconds before requesting another OTP.");
                });
    }

    private String generateOtpCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String maskPhoneNumber(String phoneNumber) {
        return "******" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
