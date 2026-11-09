package io.github.foundry4j.forge.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Validates that a string matches one of the constants of an enum.
///
/// ```java
/// public record CreateOrder(@EnumValue(OrderStatus.class) String status) {}
/// ```
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
@Documented
public @interface EnumValue {

    /// Enum whose constants are accepted.
    Class<? extends Enum<?>> value();

    /// Violation message.
    String message() default "must be one of the supported values";

    /// Validation groups.
    Class<?>[] groups() default {};

    /// Payload for custom constraints.
    Class<? extends Payload>[] payload() default {};

    /// Whether matching ignores the case of the input.
    boolean ignoreCase() default true;

    /// Whether `null` is accepted.
    boolean allowNull() default true;
}
