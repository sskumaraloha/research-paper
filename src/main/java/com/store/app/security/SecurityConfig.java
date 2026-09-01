package com.store.app.security;

import com.store.app.security.jwt.JwtAccessDeniedHandler;
import com.store.app.security.jwt.JwtAuthenticationEntryPoint;
import com.store.app.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration with two independent filter chains:
 * <ol>
 *   <li><b>REST API chain</b> ({@code /api/**}) — stateless JWT bearer
 *       authentication. No sessions, no CSRF, JSON 401/403 responses.</li>
 *   <li><b>Web chain</b> (everything else) — session-based form login for
 *       the Thymeleaf pages. The session id lives in an HttpOnly cookie
 *       (unreadable from JavaScript), and CSRF protection stays on.
 *       This is deliberately NOT JWT: storing tokens in browser-accessible
 *       storage would expose them to XSS, while server-side sessions are
 *       the established, secure mechanism for server-rendered pages.</li>
 * </ol>
 * Both chains authenticate against the same {@link DaoAuthenticationProvider}
 * (phone number + BCrypt password + account status checks), so the rules are
 * identical regardless of the entry point.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final StoreUserDetailsService userDetailsService;
    private final LoginFailureHandler loginFailureHandler;

    /** Stateless JWT chain for the REST API. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Session-based form-login chain for the Thymeleaf pages. */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(daoAuthenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/health", "/login", "/register",
                                "/verify-otp", "/forgot-password", "/verify-reset-otp",
                                "/reset-password", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("phoneNumber")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", false)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());
        return http.build();
    }

    /**
     * Shared authentication logic for form login and the API login endpoint:
     * loads the user by phone number, verifies the BCrypt password, rejects
     * disabled accounts, and requires phone verification for customers.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setPostAuthenticationChecks(userDetails -> {
            if (userDetails instanceof StoreUserDetails details
                    && details.requiresPhoneVerification()) {
                throw new PhoneNotVerifiedException(
                        "Phone number is not verified. Please verify it before logging in.");
            }
        });
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(daoAuthenticationProvider());
    }

    /**
     * BCrypt encoder used everywhere a password is stored or checked.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
