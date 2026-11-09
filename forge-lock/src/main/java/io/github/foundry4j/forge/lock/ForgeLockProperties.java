package io.github.foundry4j.forge.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Configuration of the distributed lock layer.
///
/// @param enabled       whether lock beans should be registered
/// @param provider      selected backend
/// @param keyPrefix     prefix applied to Redis keys
/// @param waitTime      default maximum time to wait for a lock
/// @param leaseTime     default lock lease, the lock expires automatically after it
/// @param retryInterval interval between two acquisition attempts
@ConfigurationProperties("forge.lock")
public record ForgeLockProperties(
        Boolean enabled,
        LockProvider provider,
        String keyPrefix,
        Duration waitTime,
        Duration leaseTime,
        Duration retryInterval
) {

    /// Applies defaults that never block a request forever.
    public ForgeLockProperties {
        enabled = enabled == null || enabled;
        provider = provider == null ? LockProvider.LOCAL : provider;
        keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "forge:lock:" : keyPrefix;
        waitTime = waitTime == null ? Duration.ofSeconds(3) : waitTime;
        leaseTime = leaseTime == null ? Duration.ofSeconds(30) : leaseTime;
        retryInterval = retryInterval == null ? Duration.ofMillis(50) : retryInterval;
    }
}
