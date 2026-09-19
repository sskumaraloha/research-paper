package com.mip.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mip.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Two-tier API rate limiting, running inside the security chain after JWT
 * authentication. Credential endpoints and the webhook get a strict per-IP budget
 * (they are what attackers hammer); everything else under /api gets a general budget
 * keyed by the authenticated user, falling back to IP. Exceeding either returns
 * 429 with a Retry-After header.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/demo-login",
            "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/refresh");

    private final RateLimiterRegistry registry;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!properties.enabled() || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter;
        if (isSensitive(path)) {
            retryAfter = registry.tryConsume("AUTH:" + clientIp(request),
                    properties.authLimit(), properties.authWindowSeconds());
        } else {
            retryAfter = registry.tryConsume("GEN:" + clientKey(request),
                    properties.generalLimit(), properties.generalWindowSeconds());
        }
        if (retryAfter < 0) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit hit on {} by {}", path, clientKey(request));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(429, "Too Many Requests",
                "Too many requests; retry in " + retryAfter + " seconds", path);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private boolean isSensitive(String path) {
        return SENSITIVE_PATHS.contains(path) || path.startsWith("/api/webhooks/");
    }

    /** Authenticated principal name when present; the client IP otherwise. */
    private String clientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
