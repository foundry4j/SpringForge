package io.github.foundry4j.forge.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/// Validator backing [Phone].
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

    private boolean allowBlank;

    @Override
    public void initialize(Phone annotation) {
        this.allowBlank = annotation.allowBlank();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return allowBlank;
        }
        return MOBILE.matcher(value).matches();
    }
}
