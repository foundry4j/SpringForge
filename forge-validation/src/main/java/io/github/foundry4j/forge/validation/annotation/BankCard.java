package io.github.foundry4j.forge.validation.annotation;

import io.github.foundry4j.forge.validation.validator.BankCardValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Validates a bank card number with the Luhn checksum.
///
/// Accepts 12 to 19 digits. `null` is accepted, use `@NotNull` when the value is
/// mandatory.
@Documented
@Constraint(validatedBy = BankCardValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BankCard {

    /// Message reported when the value is not a valid card number.
    String message() default "Invalid bank card number";

    /// Validation groups.
    Class<?>[] groups() default {};

    /// Constraint payload.
    Class<? extends Payload>[] payload() default {};
}
