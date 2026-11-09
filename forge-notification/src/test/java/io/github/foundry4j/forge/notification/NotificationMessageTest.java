package io.github.foundry4j.forge.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests the lifecycle transitions of a persisted notification.
class NotificationMessageTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

    @Test
    void startsPendingWithTheFirstAttemptScheduledNow() {
        var message = new NotificationMessage(NotificationChannel.EMAIL, "ada@example.com", null, "Body");

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(message.getAttempts()).isZero();
        assertThat(message.getSubject()).isEmpty();
        assertThat(message.getNextAttemptAt()).isNotNull();
        assertThat(message.getSentAt()).isNull();
    }

    @Test
    void marksTheNotificationAsSent() {
        var message = new NotificationMessage(NotificationChannel.SMS, "13800138000", "Hi", "Body");
        message.markFailed("boom", NOW, 3, RETRY_DELAY);

        message.markSent(NOW.plusSeconds(5));

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(message.getSentAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(message.getLastError()).isNull();
    }

    @Test
    void backsOffExponentiallyBetweenAttempts() {
        var message = new NotificationMessage(NotificationChannel.EMAIL, "ada@example.com", "Hi", "Body");

        message.markFailed("first", NOW, 3, RETRY_DELAY);

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));

        Instant second = NOW.plusSeconds(10);
        message.markFailed("second", second, 3, RETRY_DELAY);

        assertThat(message.getNextAttemptAt()).isEqualTo(second.plusSeconds(4));
    }

    @Test
    void marksTheNotificationAsFailedWhenTheBudgetIsExhausted() {
        var message = new NotificationMessage(NotificationChannel.EMAIL, "ada@example.com", "Hi", "Body");

        message.markFailed("first", NOW, 3, RETRY_DELAY);
        message.markFailed("second", NOW, 3, RETRY_DELAY);
        message.markFailed("third", NOW, 3, RETRY_DELAY);

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(message.getAttempts()).isEqualTo(3);
    }

    @Test
    void truncatesLongErrorMessages() {
        var message = new NotificationMessage(NotificationChannel.EMAIL, "ada@example.com", "Hi", "Body");

        message.markFailed("x".repeat(1200), NOW, 5, RETRY_DELAY);

        assertThat(message.getLastError()).hasSize(1000);
    }
}
