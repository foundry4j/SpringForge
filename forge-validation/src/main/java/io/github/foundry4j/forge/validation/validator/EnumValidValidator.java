package io.github.foundry4j.forge.validation.validator;

import io.github.foundry4j.forge.validation.annotation.EnumValid;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/// Checks that a string is the name of an enum constant.
///
/// @see EnumValid
public class EnumValidValidator implements ConstraintValidator<EnumValid, CharSequence> {

    private Set<String> allowedValues;
    private boolean ignoreCase;

    @Override
    public void initialize(EnumValid constraint) {
        this.ignoreCase = constraint.ignoreCase();
        this.allowedValues = Arrays.stream(constraint.value().getEnumConstants())
                .map(Enum::name)
                .map(name -> ignoreCase ? name.toLowerCase(Locale.ROOT) : name)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String candidate = value.toString();
        return allowedValues.contains(ignoreCase ? candidate.toLowerCase(Locale.ROOT) : candidate);
    }
}
