package com.store.app.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator
        implements ConstraintValidator<PasswordMatches, PasswordConfirmable> {

    @Override
    public boolean isValid(PasswordConfirmable request, ConstraintValidatorContext context) {
        // Leave null/blank handling to the field-level @NotBlank constraints.
        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            return true;
        }

        boolean matches = request.getPassword().equals(request.getConfirmPassword());
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return matches;
    }
}
