package io.github.foundry4j.forge.idempotency;

/// State stored for an idempotency key.
///
/// @param state     current state
/// @param payload   serialized method result, `null` while the call is still processing
/// @param expiresAt expiration timestamp in milliseconds, checked lazily
public record IdempotentEntry(State state, byte[] payload, long expiresAt) {

    /// Lifecycle of an idempotency key.
    public enum State {

        /// The method is running and has not produced a result yet.
        PROCESSING,

        /// The method finished and [payload] can be replayed.
        DONE
    }
}
