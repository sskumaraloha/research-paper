package com.mip.user.service;

import com.mip.exception.BusinessRuleViolationException;
import com.mip.exception.DuplicateResourceException;
import com.mip.exception.InvalidRequestException;
import com.mip.exception.RateLimitedException;
import com.mip.exception.UnauthorizedException;
import com.mip.notification.email.EmailService;
import com.mip.security.DemoProperties;
import com.mip.security.JwtTokenService;
import com.mip.security.PasswordResetProperties;
import com.mip.user.dto.AuthResponse;
import com.mip.user.dto.ForgotPasswordRequest;
import com.mip.user.dto.LoginRequest;
import com.mip.user.dto.RefreshRequest;
import com.mip.user.dto.RegistrationRequest;
import com.mip.user.dto.ResetPasswordRequest;
import com.mip.user.dto.SimpleMessageResponse;
import com.mip.user.dto.UserProfileResponse;
import com.mip.user.entity.PasswordResetToken;
import com.mip.user.entity.RefreshToken;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import com.mip.user.repository.PasswordResetTokenRepository;
import com.mip.user.repository.RefreshTokenRepository;
import com.mip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String FORGOT_PASSWORD_REPLY =
            "If an account exists for this email, a password reset link has been sent";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EmailService emailService;
    private final DemoProperties demoProperties;
    private final PasswordResetProperties passwordResetProperties;

    /**
     * Self-registration. New accounts start as VIEWER with no plants; an admin promotes
     * them and assigns plants. The response logs the new user straight in.
     */
    @Transactional
    public AuthResponse register(RegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        User user = new User(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), RoleName.VIEWER);
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            String phone = request.phoneNumber().trim();
            if (userRepository.existsByPhoneNumber(phone)) {
                throw new DuplicateResourceException("This phone number is already registered");
            }
            user.setPhoneNumber(phone);
        }
        User saved = userRepository.save(user);
        log.info("User {} self-registered", saved.getId());
        return issueTokens(saved);
    }

    /**
     * Sends a password-reset link by email. The reply never reveals whether the email
     * exists; the raw token lives only inside the link, its hash in the database.
     */
    @Transactional
    public SimpleMessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(User::isActive)
                .ifPresent(this::issueResetLink);
        return new SimpleMessageResponse(FORGOT_PASSWORD_REPLY);
    }

    /** Lets the reset page tell the user their link is invalid before they type a password. */
    @Transactional(readOnly = true)
    public SimpleMessageResponse validateResetToken(String token) {
        requireUsableResetToken(token);
        return new SimpleMessageResponse("Token is valid");
    }

    /**
     * Consumes the token, sets the new password and revokes every refresh token.
     * Access tokens issued before the reset remain valid until their short TTL expires.
     */
    @Transactional
    public SimpleMessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = requireUsableResetToken(request.token());
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Password reset completed for user {}", user.getId());
        return new SimpleMessageResponse("Password updated. You can now log in with your new password");
    }

    private void issueResetLink(User user) {
        long recentRequests = passwordResetTokenRepository.countByUserIdAndCreatedAtAfter(
                user.getId(), Instant.now().minus(Duration.ofHours(1)));
        if (recentRequests >= passwordResetProperties.maxRequestsPerHour()) {
            throw new RateLimitedException(
                    "Too many password reset requests; try again in an hour");
        }
        String rawToken = jwtTokenService.generateRefreshToken();
        passwordResetTokenRepository.save(new PasswordResetToken(user,
                jwtTokenService.hashRefreshToken(rawToken),
                Instant.now().plus(Duration.ofMinutes(passwordResetProperties.expiryMinutes()))));
        String link = passwordResetProperties.url() + "?token=" + rawToken;
        emailService.send(user.getEmail(), "Reset your Maintenance Intelligence Platform password",
                "Hi " + user.getFullName() + ",\n\n"
                        + "We received a request to reset your password. Open the link below to "
                        + "choose a new one:\n\n" + link + "\n\n"
                        + "The link is valid for " + passwordResetProperties.expiryMinutes()
                        + " minutes and can be used once. If you did not request this, "
                        + "you can safely ignore this email.");
        log.info("Password reset link issued for user {}", user.getId());
    }

    private PasswordResetToken requireUsableResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRequestException("Reset token is required");
        }
        return passwordResetTokenRepository
                .findByTokenHash(jwtTokenService.hashRefreshToken(token.trim()))
                .filter(PasswordResetToken::isUsable)
                .filter(t -> t.getUser().isActive())
                .orElseThrow(() -> new UnauthorizedException(
                        "This reset link is invalid or has expired; request a new one"));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("This account has been deactivated");
        }
        return issueTokens(user);
    }

    /** Logs in the pre-seeded demo user. Available only when app.demo.enabled=true. */
    @Transactional
    public AuthResponse demoLogin() {
        if (!demoProperties.enabled()) {
            throw new BusinessRuleViolationException("Demo login is disabled in this environment");
        }
        User user = userRepository.findByEmailIgnoreCase(demoProperties.email())
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessRuleViolationException("Demo user is not provisioned"));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(jwtTokenService.hashRefreshToken(request.refreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (!stored.isUsable()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }
        User user = stored.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("This account has been deactivated");
        }
        // rotation: the presented token is consumed and a new one issued
        stored.setRevoked(true);
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(jwtTokenService.hashRefreshToken(request.refreshToken()))
                .ifPresent(token -> token.setRevoked(true));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse currentUser(Long userId) {
        return userService.getProfile(userId);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtTokenService.createAccessToken(user);
        String rawRefresh = jwtTokenService.generateRefreshToken();
        refreshTokenRepository.save(new RefreshToken(user,
                jwtTokenService.hashRefreshToken(rawRefresh), jwtTokenService.refreshTokenExpiry()));
        log.debug("Issued tokens for user {}", user.getId());
        return new AuthResponse(accessToken, rawRefresh, jwtTokenService.accessTokenValiditySeconds(),
                userService.getProfile(user.getId()));
    }
}
