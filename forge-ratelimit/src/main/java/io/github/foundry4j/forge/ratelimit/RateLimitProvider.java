package io.github.foundry4j.forge.ratelimit;

/// Backend used by the rate limiting layer.
public enum RateLimitProvider {

    /// Per instance counters kept in memory.
    LOCAL,

    /// Shared counters stored in Redis, required for multi instance deployments.
    REDIS
}
