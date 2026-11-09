package io.github.foundry4j.forge.idempotency;

/// Backend used to store idempotency state.
public enum IdempotencyProvider {

    /// Per instance state kept in memory.
    LOCAL,

    /// Shared state stored in Redis, required for multi instance deployments.
    REDIS
}
