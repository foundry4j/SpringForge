package io.github.foundry4j.forge.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Configuration of the transactional outbox.
///
/// @param enabled      whether the outbox layer is registered
/// @param relayEnabled whether the background relay is started
/// @param batchSize    maximum number of events claimed per relay run
/// @param maxAttempts  delivery attempts before an event is marked as failed
/// @param retryDelay   base delay used by the exponential backoff
/// @param pollInterval pause between two relay runs
/// @param stuckTimeout age after which a `PROCESSING` event is considered abandoned
/// @param retention    how long `SENT` events are kept before they are purged
@ConfigurationProperties("forge.outbox")
public record OutboxProperties(
        Boolean enabled,
        Boolean relayEnabled,
        Integer batchSize,
        Integer maxAttempts,
        Duration retryDelay,
        Duration pollInterval,
        Duration stuckTimeout,
        Duration retention
) {

    /// Applies sensible defaults for every optional property.
    public OutboxProperties {
        enabled = enabled == null || enabled;
        relayEnabled = relayEnabled == null || relayEnabled;
        batchSize = batchSize == null || batchSize < 1 ? 50 : batchSize;
        maxAttempts = maxAttempts == null || maxAttempts < 1 ? 8 : maxAttempts;
        retryDelay = retryDelay == null ? Duration.ofSeconds(30) : retryDelay;
        pollInterval = pollInterval == null ? Duration.ofSeconds(2) : pollInterval;
        stuckTimeout = stuckTimeout == null ? Duration.ofMinutes(5) : stuckTimeout;
        retention = retention == null ? Duration.ofDays(7) : retention;
    }
}
