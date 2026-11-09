package io.github.foundry4j.forge.outbox;

import io.github.foundry4j.forge.messaging.ForgeMessage;

/// Delivers an outbox event to its final destination.
@FunctionalInterface
public interface OutboxTransport {

    /// Delivers the message.
    ///
    /// Implementations must throw when the delivery failed so that the relay can
    /// schedule a retry.
    void deliver(ForgeMessage message);
}
