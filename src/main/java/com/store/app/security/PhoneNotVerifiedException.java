package com.store.app.security;

import org.springframework.security.authentication.AccountStatusException;

/**
 * Raised during authentication when a customer account has not
 * completed phone verification yet.
 */
public class PhoneNotVerifiedException extends AccountStatusException {

    public PhoneNotVerifiedException(String message) {
        super(message);
    }
}
