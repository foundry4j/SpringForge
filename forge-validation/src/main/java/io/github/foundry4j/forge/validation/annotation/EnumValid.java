package io.github.foundry4j.forge.validation.annotation;

import io.github.foundry4j.forge.validation.validator.EnumValidValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Validates that a string is the name of a constant of the given enum.
///
/// ```java
/// @EnumValid(OrderStatus.class)
/// private String status;
/// ```
///
/// `null` is accepted, use `@NotNull` when the value is mandatory.
@Documented
@Constraint(validatedBy = EnumValidValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValid {

    /// Enum whose constants are accepted.
    Class<? extends Enum<?>> value();

    /// Whether the comparison ignores case.
    boolean ignoreCase() default false;

    /// Message reported when the value is not a valid constant name.
    String message() default "Must be a valid enum value";

    /// Validation groups.
    Class<?>[] groups() default {};

    /// Constraint payload.
    Class<? extends Payload>[] payload() default {};
}
