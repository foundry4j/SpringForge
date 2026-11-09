package io.github.foundry4j.forge.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/// Validator backing [EnumValue].
public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Set<String> allowed;
    private boolean ignoreCase;
    private boolean allowNull;

    @Override
    public void initialize(EnumValue annotation) {
        this.allowed = Arrays.stream(annotation.value().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        this.ignoreCase = annotation.ignoreCase();
        this.allowNull = annotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }
        if (ignoreCase) {
            return allowed.contains(value.toUpperCase(Locale.ROOT));
        }
        return allowed.contains(value);
    }
}
