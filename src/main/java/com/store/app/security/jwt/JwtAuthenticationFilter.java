package com.store.app.security.jwt;

import com.store.app.security.StoreUserDetails;
import com.store.app.security.StoreUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Authenticates REST API requests carrying a {@code Authorization: Bearer}
 * JWT. The user is re-loaded from the database on each request so that
 * disabling an account takes effect immediately, even for tokens that
 * have not yet expired.
 * <p>
 * Invalid or missing tokens are not rejected here — the request simply
 * proceeds unauthenticated, and {@link JwtAuthenticationEntryPoint}
 * produces the 401 when a protected endpoint is hit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final StoreUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtService.parseClaims(token);
            StoreUserDetails userDetails =
                    userDetailsService.loadUserByUsername(claims.getSubject());

            if (userDetails.isEnabled()
                    && !userDetails.requiresPhoneVerification()
                    && !issuedBeforePasswordChange(claims, userDetails)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | UsernameNotFoundException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * A password change revokes all previously issued tokens: any JWT
     * whose issue time predates the change is rejected.
     */
    private boolean issuedBeforePasswordChange(Claims claims, StoreUserDetails userDetails) {
        LocalDateTime passwordChangedAt = userDetails.getUser().getPasswordChangedAt();
        Date issuedAt = claims.getIssuedAt();
        if (passwordChangedAt == null || issuedAt == null) {
            return false;
        }
        Instant changedAt = passwordChangedAt.atZone(ZoneId.systemDefault()).toInstant();
        return issuedAt.toInstant().isBefore(changedAt);
    }
}
