package com.store.app.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint verifying that a registration request's
 * {@code password} and {@code confirmPassword} fields match.
 * The violation is reported on the {@code confirmPassword} field.
 */
@Documented
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {

    String message() default "Password and confirm password do not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
