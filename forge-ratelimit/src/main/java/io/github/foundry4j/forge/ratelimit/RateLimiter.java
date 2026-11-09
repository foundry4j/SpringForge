package io.github.foundry4j.forge.ratelimit;

/// Rate limiting backend SPI.
///
/// The default implementation is the single node [InMemoryRateLimiter]. Applications
/// that need a cluster wide quota can declare their own bean, for example one backed by
/// Redis, without touching the annotation or the aspect.
public interface RateLimiter {

    /// Tries to acquire one permit.
    ///
    /// @param key           limit key, already namespaced by method and strategy
    /// @param permits       number of calls allowed inside the window
    /// @param windowSeconds window length in seconds
    /// @return `true` when the call is allowed, `false` when the quota is exhausted
    boolean tryAcquire(String key, int permits, long windowSeconds);
}
