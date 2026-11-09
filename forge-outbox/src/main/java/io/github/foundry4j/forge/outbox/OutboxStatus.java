package io.github.foundry4j.forge.outbox;

/// Lifecycle of a stored outbox event.
public enum OutboxStatus {

    /// Waiting to be claimed by the relay.
    PENDING,

    /// Claimed by a relay and currently being delivered.
    PROCESSING,

    /// Delivered successfully; kept until the retention window elapses.
    SENT,

    /// Delivery failed permanently, the retry budget is exhausted.
    FAILED
}
