package io.github.foundry4j.forge.outbox;

import io.github.foundry4j.forge.messaging.ForgeMessage;
import io.github.foundry4j.forge.messaging.MessagePublisher;

/// Adapts the configured [MessagePublisher] to the outbox transport contract.
public class MessagingOutboxTransport implements OutboxTransport {

    private final MessagePublisher publisher;

    /// Creates the transport.
    public MessagingOutboxTransport(MessagePublisher publisher) {
        this.publisher = publisher;
    }

    /// {@inheritDoc}
    @Override
    public void deliver(ForgeMessage message) {
        publisher.publish(message);
    }
}
