package io.github.foundry4j.forge.core.support;

import java.util.HexFormat;
import java.util.UUID;
import java.util.random.RandomGenerator;

/// Identifier helpers used by logs, messages and error responses.
public final class Ids {

    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();
    private static final HexFormat HEX = HexFormat.of();

    private Ids() {
    }

    /// Returns a canonical UUID v4 string.
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /// Returns a compact identifier without dashes, handy for message and event keys.
    public static String compactId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /// Returns a 16 character hexadecimal trace identifier.
    public static String traceId() {
        return HEX.toHexDigits(RANDOM.nextLong());
    }

    /// Returns a 32 character hexadecimal identifier with 128 bits of entropy.
    public static String longId() {
        return HEX.toHexDigits(RANDOM.nextLong()) + HEX.toHexDigits(RANDOM.nextLong());
    }
}
