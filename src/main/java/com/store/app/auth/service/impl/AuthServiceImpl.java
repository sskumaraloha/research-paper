package com.store.app.auth.service.impl;

import com.store.app.auth.dto.LoginRequest;
import com.store.app.auth.dto.LoginResponse;
import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.dto.RegistrationResponse;
import com.store.app.auth.service.AuthService;
import com.store.app.exception.AuthenticationFailedException;
import com.store.app.exception.PasswordMismatchException;
import com.store.app.security.PhoneNotVerifiedException;
import com.store.app.security.StoreUserDetails;
import com.store.app.security.jwt.JwtService;
import com.store.app.user.dto.CreateUserRequest;
import com.store.app.user.dto.UserResponse;
import com.store.app.user.mapper.UserMapper;
import com.store.app.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REGISTRATION_SUCCESS_MESSAGE =
            "Registration successful. Verify your phone number, then log in.";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public RegistrationResponse registerCustomer(RegistrationRequest request) {
        // Defense in depth: the @PasswordMatches constraint normally catches this
        // at the controller boundary, but the service must not rely on callers
        // having validated the request.
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Password and confirm password do not match");
        }

        UserResponse user = userService.createUser(new CreateUserRequest(
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber(),
                request.getEmail(),
                request.getPassword()
        ));

        return new RegistrationResponse(
                user.id(),
                user.firstName(),
                user.lastName(),
                user.phoneNumber(),
                user.email(),
                user.phoneVerified(),
                user.roles(),
                REGISTRATION_SUCCESS_MESSAGE
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.phoneNumber(), request.password()));
        } catch (BadCredentialsException ex) {
            throw new AuthenticationFailedException("Invalid phone number or password");
        } catch (DisabledException ex) {
            throw new AuthenticationFailedException("Account is disabled");
        } catch (PhoneNotVerifiedException ex) {
            throw new AuthenticationFailedException(ex.getMessage());
        } catch (AuthenticationException ex) {
            throw new AuthenticationFailedException("Authentication failed");
        }

        StoreUserDetails userDetails = (StoreUserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(userDetails.getUser());

        return LoginResponse.bearer(
                accessToken,
                jwtService.getExpiresInSeconds(),
                userMapper.toResponse(userDetails.getUser()));
    }
}
