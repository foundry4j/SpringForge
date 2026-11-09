package io.github.foundry4j.forge.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Validates a Chinese mainland mobile phone number.
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented
public @interface Phone {

    /// Violation message.
    String message() default "must be a valid mobile phone number";

    /// Validation groups.
    Class<?>[] groups() default {};

    /// Payload for custom constraints.
    Class<? extends Payload>[] payload() default {};

    /// Whether `null` and blank values are accepted.
    boolean allowBlank() default true;
}
