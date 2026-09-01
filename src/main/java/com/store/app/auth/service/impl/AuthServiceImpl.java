package com.store.app.auth.service.impl;

import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.dto.RegistrationResponse;
import com.store.app.auth.service.AuthService;
import com.store.app.exception.PasswordMismatchException;
import com.store.app.user.dto.CreateUserRequest;
import com.store.app.user.dto.UserResponse;
import com.store.app.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REGISTRATION_SUCCESS_MESSAGE =
            "Registration successful. You can log in once login is available; "
                    + "phone verification is pending.";

    private final UserService userService;

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
}
