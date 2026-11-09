package io.github.foundry4j.forge.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the outbox event lifecycle and its retry policy.
class OutboxEventTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void startsPendingAndBecomesSent() {
        var event = new OutboxEvent("m-1", "order.created", "orders", "{}", "{}", "acme", NOW);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW);

        event.markProcessing(NOW);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);

        event.markSent(NOW.plusSeconds(1));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getSentAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void backsOffExponentiallyBetweenAttempts() {
        var event = new OutboxEvent("m-2", "type", "", "{}", "{}", null, NOW);
        var delay = Duration.ofSeconds(10);

        event.markFailed("boom", NOW, 5, delay);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(10));

        event.markFailed("boom", NOW, 5, delay);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(20));
        assertThat(event.getAttempts()).isEqualTo(2);
    }

    @Test
    void becomesFailedWhenTheBudgetIsExhausted() {
        var event = new OutboxEvent("m-3", "type", "", "{}", "{}", null, NOW);

        event.markFailed("boom", NOW, 2, Duration.ofSeconds(1));
        event.markFailed("boom", NOW, 2, Duration.ofSeconds(1));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(2);
    }

    @Test
    void truncatesLongErrorMessages() {
        var event = new OutboxEvent("m-4", "type", "", "{}", "{}", null, NOW);

        event.markFailed("x".repeat(2000), NOW, 5, Duration.ofSeconds(1));

        assertThat(event.getLastError()).hasSize(1000);
    }
}
