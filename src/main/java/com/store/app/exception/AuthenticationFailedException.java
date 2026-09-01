package com.store.app.exception;

/**
 * Thrown when API login fails (unknown user, wrong password, disabled
 * account, or unverified phone). Mapped to HTTP 401.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
