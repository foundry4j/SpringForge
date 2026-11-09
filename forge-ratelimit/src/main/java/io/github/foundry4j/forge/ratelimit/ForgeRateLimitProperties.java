package io.github.foundry4j.forge.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the rate limiting layer.
///
/// @param provider  selected backend, local counters by default
/// @param keyPrefix prefix applied to Redis keys when the Redis backend is used
@ConfigurationProperties("forge.ratelimit")
public record ForgeRateLimitProperties(RateLimitProvider provider, String keyPrefix) {

    /// Applies defaults that keep the module usable without any configuration.
    public ForgeRateLimitProperties {
        provider = provider == null ? RateLimitProvider.LOCAL : provider;
        keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "forge:ratelimit:" : keyPrefix;
    }
}
