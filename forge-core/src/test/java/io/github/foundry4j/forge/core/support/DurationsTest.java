package io.github.foundry4j.forge.core.support;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the duration literal parser.
class DurationsTest {

    private static final Duration FALLBACK = Duration.ofSeconds(1);

    @Test
    void parsesCompactSuffixes() {
        assertThat(Durations.parse("500ms", FALLBACK)).isEqualTo(Duration.ofMillis(500));
        assertThat(Durations.parse("30s", FALLBACK)).isEqualTo(Duration.ofSeconds(30));
        assertThat(Durations.parse("5m", FALLBACK)).isEqualTo(Duration.ofMinutes(5));
        assertThat(Durations.parse("2h", FALLBACK)).isEqualTo(Duration.ofHours(2));
        assertThat(Durations.parse("1d", FALLBACK)).isEqualTo(Duration.ofDays(1));
        assertThat(Durations.parse("150", FALLBACK)).isEqualTo(Duration.ofMillis(150));
    }

    @Test
    void parsesIsoAndFallsBackForInvalidInput() {
        assertThat(Durations.parse("PT10S", FALLBACK)).isEqualTo(Duration.ofSeconds(10));
        assertThat(Durations.parse("not-a-duration", FALLBACK)).isEqualTo(FALLBACK);
        assertThat(Durations.parse(null, FALLBACK)).isEqualTo(FALLBACK);
        assertThat(Durations.parse("  ", FALLBACK)).isEqualTo(FALLBACK);
    }
}
