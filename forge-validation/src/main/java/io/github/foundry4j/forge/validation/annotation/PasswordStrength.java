package io.github.foundry4j.forge.validation.annotation;

import io.github.foundry4j.forge.validation.validator.PasswordStrengthValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Validates password length and character composition.
///
/// By default a password must be 8 to 64 characters long and contain at least one
/// letter and one digit. `null` is accepted, use `@NotNull` when the value is
/// mandatory.
@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {

    /// Message reported when the password is too weak.
    String message() default "Password does not meet the strength requirements";

    /// Minimum length.
    int min() default 8;

    /// Maximum length.
    int max() default 64;

    /// Whether at least one digit is required.
    boolean requireDigit() default true;

    /// Whether at least one letter is required.
    boolean requireLetter() default true;

    /// Whether at least one special character is required.
    boolean requireSpecial() default false;

    /// Validation groups.
    Class<?>[] groups() default {};

    /// Constraint payload.
    Class<? extends Payload>[] payload() default {};
}
