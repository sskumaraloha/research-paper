package com.mip.user.service;

import com.mip.exception.BusinessRuleViolationException;
import com.mip.exception.UnauthorizedException;
import com.mip.security.DemoProperties;
import com.mip.security.JwtTokenService;
import com.mip.user.dto.AuthResponse;
import com.mip.user.dto.LoginRequest;
import com.mip.user.dto.RefreshRequest;
import com.mip.user.dto.UserProfileResponse;
import com.mip.user.entity.RefreshToken;
import com.mip.user.entity.User;
import com.mip.user.repository.RefreshTokenRepository;
import com.mip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final DemoProperties demoProperties;

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
