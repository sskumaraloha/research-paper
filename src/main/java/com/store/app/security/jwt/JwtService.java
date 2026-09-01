package com.store.app.security.jwt;

import com.store.app.user.entity.Role;
import com.store.app.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Issues and validates HMAC-signed JWT access tokens for the REST API.
 * <p>
 * Tokens carry the user id ({@code userId} claim), phone number
 * (subject), and role names ({@code roles} claim) — never the password
 * or any other sensitive data.
 */
@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLES = "roles";

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(JwtProperties properties) {
        if (!StringUtils.hasText(properties.secret())) {
            throw new IllegalStateException(
                    "app.jwt.secret must be configured (base64-encoded, >= 256 bits)");
        }
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expirationMinutes = properties.expirationMinutes();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .toList();

        return Jwts.builder()
                .subject(user.getPhoneNumber())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(getExpiresInSeconds())))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and verifies a token, returning its claims.
     *
     * @throws JwtException if the token is malformed, tampered with, or expired
     */
    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiresInSeconds() {
        return expirationMinutes * 60;
    }
}
