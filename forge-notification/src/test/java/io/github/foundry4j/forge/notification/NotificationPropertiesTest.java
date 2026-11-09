package io.github.foundry4j.forge.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests the defaults of the notification configuration.
class NotificationPropertiesTest {

    @Test
    void appliesDefaultsForMissingValues() {
        var properties = new NotificationProperties(null, null, null, null, null, null);

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.retryEnabled()).isTrue();
        assertThat(properties.batchSize()).isEqualTo(50);
        assertThat(properties.maxAttempts()).isEqualTo(3);
        assertThat(properties.retryDelay()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.pollInterval()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void keepsExplicitValues() {
        var properties = new NotificationProperties(false, false, 5, 9,
                Duration.ofSeconds(10), Duration.ofSeconds(3));

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.retryEnabled()).isFalse();
        assertThat(properties.batchSize()).isEqualTo(5);
        assertThat(properties.maxAttempts()).isEqualTo(9);
    }
}
