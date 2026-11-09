package io.github.foundry4j.forge.validation.validator;

import io.github.foundry4j.forge.validation.annotation.BankCard;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/// Checks bank card numbers with the Luhn algorithm.
///
/// @see BankCard
public class BankCardValidator implements ConstraintValidator<BankCard, CharSequence> {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String number = value.toString();
        if (!number.matches("^\\d{12,19}$")) {
            return false;
        }
        return luhnValid(number);
    }

    private boolean luhnValid(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
