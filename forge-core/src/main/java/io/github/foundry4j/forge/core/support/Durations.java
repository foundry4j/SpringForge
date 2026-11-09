package io.github.foundry4j.forge.core.support;

import java.time.Duration;
import java.util.Locale;

/// Parses the compact duration literals used in annotations.
///
/// Supported forms are ISO-8601 (`PT30S`) and the friendlier suffixes `ms`, `s`,
/// `m`, `h`, `d`, for example `500ms`, `30s` or `5m`.
public final class Durations {

    private Durations() {
    }

    /// Parses `value`, returning `fallback` when the value is blank or invalid.
    public static Duration parse(String value, Duration fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String text = value.trim().toUpperCase(Locale.ROOT);
        try {
            if (text.startsWith("P")) {
                return Duration.parse(text);
            }
            if (text.endsWith("MS")) {
                return Duration.ofMillis(Long.parseLong(text.substring(0, text.length() - 2)));
            }
            if (text.endsWith("S")) {
                return Duration.ofSeconds(Long.parseLong(text.substring(0, text.length() - 1)));
            }
            if (text.endsWith("M")) {
                return Duration.ofMinutes(Long.parseLong(text.substring(0, text.length() - 1)));
            }
            if (text.endsWith("H")) {
                return Duration.ofHours(Long.parseLong(text.substring(0, text.length() - 1)));
            }
            if (text.endsWith("D")) {
                return Duration.ofDays(Long.parseLong(text.substring(0, text.length() - 1)));
            }
            return Duration.ofMillis(Long.parseLong(text));
        } catch (RuntimeException _) {
            return fallback;
        }
    }
}
