package com.store.app.auth.validation;

/**
 * Contract for request DTOs carrying a password plus its confirmation,
 * validated by {@link PasswordMatches}.
 */
public interface PasswordConfirmable {

    String getPassword();

    String getConfirmPassword();
}
