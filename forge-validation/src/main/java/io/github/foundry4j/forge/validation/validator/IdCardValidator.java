package io.github.foundry4j.forge.validation.validator;

import io.github.foundry4j.forge.validation.annotation.IdCard;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/// Checks Chinese resident identity card numbers.
///
/// @see IdCard
public class IdCardValidator implements ConstraintValidator<IdCard, CharSequence> {

    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
    private static final int[] DAYS_OF_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String id = value.toString().toUpperCase();
        if (!id.matches("^\\d{17}[0-9X]$")) {
            return false;
        }
        return validBirthday(id.substring(6, 14)) && validCheckCode(id);
    }

    private boolean validCheckCode(String id) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id.charAt(i) - '0') * WEIGHTS[i];
        }
        return CHECK_CODES[sum % 11] == id.charAt(17);
    }

    private boolean validBirthday(String yyyymmdd) {
        int year = Integer.parseInt(yyyymmdd.substring(0, 4));
        int month = Integer.parseInt(yyyymmdd.substring(4, 6));
        int day = Integer.parseInt(yyyymmdd.substring(6, 8));
        if (year < 1900 || month < 1 || month > 12) {
            return false;
        }
        int maxDay = DAYS_OF_MONTH[month - 1];
        if (month == 2 && isLeapYear(year)) {
            maxDay = 29;
        }
        return day >= 1 && day <= maxDay;
    }

    private boolean isLeapYear(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }
}
