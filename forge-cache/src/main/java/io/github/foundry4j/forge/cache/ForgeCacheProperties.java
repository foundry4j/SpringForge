package io.github.foundry4j.forge.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Configuration of the forge cache layer.
///
/// @param provider     selected topology
/// @param keyPrefix    prefix applied to every Redis key
/// @param localTtl     time to live of the Caffeine tier
/// @param remoteTtl    time to live of the Redis tier
/// @param localMaxSize maximum number of entries kept in the Caffeine tier
@ConfigurationProperties("forge.cache")
public record ForgeCacheProperties(
        CacheProvider provider,
        String keyPrefix,
        Duration localTtl,
        Duration remoteTtl,
        long localMaxSize
) {

    /// Applies defaults that safely work without any Redis instance.
    public ForgeCacheProperties {
        provider = provider == null ? CacheProvider.LOCAL : provider;
        keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "forge:" : keyPrefix;
        localTtl = localTtl == null ? Duration.ofSeconds(30) : localTtl;
        remoteTtl = remoteTtl == null ? Duration.ofMinutes(10) : remoteTtl;
        localMaxSize = localMaxSize <= 0 ? 10_000L : localMaxSize;
    }
}
