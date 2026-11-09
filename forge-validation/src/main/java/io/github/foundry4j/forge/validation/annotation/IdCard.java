package io.github.foundry4j.forge.validation.annotation;

import io.github.foundry4j.forge.validation.validator.IdCardValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Validates the 18 character Chinese resident identity card number.
///
/// The check covers the numeric format, a plausible birth date and the weighted
/// check digit defined by GB 11643-1999. `null` is accepted, use `@NotNull` when the
/// value is mandatory.
@Documented
@Constraint(validatedBy = IdCardValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdCard {

    /// Message reported when the value is not a valid identity card number.
    String message() default "Invalid identity card number";

    /// Validation groups.
    Class<?>[] groups() default {};

    /// Constraint payload.
    Class<? extends Payload>[] payload() default {};
}
