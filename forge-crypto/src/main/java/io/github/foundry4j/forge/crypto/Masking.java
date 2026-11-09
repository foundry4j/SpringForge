package io.github.foundry4j.forge.crypto;

/// Masks values before they reach a log or a JSON response.
public final class Masking {

    private Masking() {
    }

    /// Masks a value according to the strategy.
    public static String mask(String value, SensitiveType type) {
        if (value == null || value.isEmpty() || type == null || type == SensitiveType.NONE) {
            return value;
        }
        return switch (type) {
            case NONE -> value;
            case NAME -> keep(value, 1, 0);
            case PHONE -> keep(value, 3, 4);
            case ID_CARD -> keep(value, 6, 4);
            case BANK_CARD -> keep(value, 0, 4);
            case EMAIL -> maskEmail(value);
            case ADDRESS -> keep(value, 6, 0);
            case PASSWORD -> "*".repeat(value.length());
        };
    }

    private static String keep(String value, int prefix, int suffix) {
        int length = value.length();
        if (length <= prefix + suffix) {
            return "*".repeat(length);
        }
        String head = prefix == 0 ? "" : value.substring(0, prefix);
        String tail = suffix == 0 ? "" : value.substring(length - suffix);
        return head + "*".repeat(length - prefix - suffix) + tail;
    }

    private static String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return keep(value, 1, 0);
        }
        String local = value.substring(0, at);
        String domain = value.substring(at);
        return local.charAt(0) + "***" + domain;
    }
}
