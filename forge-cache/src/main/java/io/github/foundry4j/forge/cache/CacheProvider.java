package io.github.foundry4j.forge.cache;

/// Supported cache topologies.
public enum CacheProvider {

    /// In-process Caffeine cache only. Best for single node deployments and tests.
    LOCAL,

    /// Redis only. Shared between all nodes.
    REDIS,

    /// Caffeine in front of Redis. Lowest latency with shared invalidation.
    TIERED
}
