package io.github.foundry4j.forge.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the idempotency layer.
///
/// @param provider  selected backend, in-memory state by default
/// @param keyPrefix prefix applied to Redis keys when the Redis backend is used
@ConfigurationProperties("forge.idempotency")
public record ForgeIdempotencyProperties(IdempotencyProvider provider, String keyPrefix) {

    /// Applies defaults that keep the module usable without any configuration.
    public ForgeIdempotencyProperties {
        provider = provider == null ? IdempotencyProvider.LOCAL : provider;
        keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "forge:idempotency:" : keyPrefix;
    }
}
