package com.store.app.auth.service;

import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.dto.RegistrationResponse;

/**
 * Authentication-related operations (registration; login arrives in a later phase).
 */
public interface AuthService {

    /**
     * Registers a new customer account with {@code ROLE_CUSTOMER}.
     * The password is BCrypt-encoded and the phone starts unverified.
     *
     * @throws com.store.app.exception.PasswordMismatchException
     *         if password and confirmPassword differ
     * @throws com.store.app.exception.DuplicateResourceException
     *         if the email or phone number is already registered
     */
    RegistrationResponse registerCustomer(RegistrationRequest request);
}
