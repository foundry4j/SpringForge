package io.github.foundry4j.forge.cache.store;

import java.time.Duration;
import java.util.Optional;

/// Minimal key/value contract used by idempotency, rate limiting and locking helpers.
///
/// Implementations must be safe for concurrent use. The framework ships a Caffeine
/// backed implementation for single node deployments and a Redis backed
/// implementation for clustered deployments.
public interface KeyValueStore {

    /// Returns the stored value, if any.
    Optional<String> get(String key);

    /// Stores a value with a time to live.
    void set(String key, String value, Duration ttl);

    /// Stores the value only when the key is absent.
    ///
    /// @return `true` when the value was written, `false` when the key already existed
    boolean setIfAbsent(String key, String value, Duration ttl);

    /// Atomically increments the counter stored at `key`, creating it with `ttl` on
    /// the first increment.
    ///
    /// @return the value after the increment
    long increment(String key, Duration ttl);

    /// Removes the key.
    ///
    /// @return `true` when a value was removed
    boolean delete(String key);

    /// Whether the key currently exists.
    default boolean exists(String key) {
        return get(key).isPresent();
    }
}
