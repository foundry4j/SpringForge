package io.github.foundry4j.forge.messaging;

/// Publishes [ForgeMessage] instances to a broker.
public interface MessagePublisher {

    /// Publishes a message.
    ///
    /// @throws RuntimeException when the broker rejects the message
    void publish(ForgeMessage message);
}
