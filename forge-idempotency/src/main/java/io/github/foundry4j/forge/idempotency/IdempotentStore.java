package io.github.foundry4j.forge.idempotency;

/// Idempotency state storage SPI.
///
/// The default implementation is the single node [InMemoryIdempotentStore]. Applications
/// that need shared state can declare their own bean, for example one backed by Redis.
public interface IdempotentStore {

    /// Tries to reserve the key in `PROCESSING` state.
    ///
    /// @param key        idempotency key
    /// @param ttlSeconds reservation lifetime in seconds
    /// @return `null` when the key was missing or expired (the caller is the first one),
    ///         otherwise the existing entry, either `PROCESSING` or `DONE`
    IdempotentEntry tryAcquire(String key, long ttlSeconds);

    /// Stores the serialized result and marks the key as `DONE`.
    ///
    /// @param key        idempotency key
    /// @param payload    serialized result
    /// @param ttlSeconds how long the result stays replayable, in seconds
    void markDone(String key, byte[] payload, long ttlSeconds);

    /// Releases the key after a failed call so the caller may retry.
    ///
    /// @param key idempotency key
    void markFailed(String key);
}
