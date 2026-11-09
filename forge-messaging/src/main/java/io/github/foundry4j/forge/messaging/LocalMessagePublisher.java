package io.github.foundry4j.forge.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/// Default publisher that keeps messages in memory and dispatches them locally.
///
/// It is used for local development and tests and is replaced as soon as a broker
/// publisher is configured. The last hundred messages are retained so a test can
/// assert what a use case published.
public class LocalMessagePublisher implements MessagePublisher {

    private static final Logger LOG = LoggerFactory.getLogger(LocalMessagePublisher.class);
    private static final int MAX_HISTORY = 100;

    private final LocalMessageBus bus;
    private final Deque<MessageEnvelope> history = new ArrayDeque<>(MAX_HISTORY);

    /// Creates the publisher.
    public LocalMessagePublisher(LocalMessageBus bus) {
        this.bus = bus;
    }

    @Override
    public void publish(ForgeMessage message) {
        String destination = message.destination() == null || message.destination().isBlank()
                ? message.type()
                : message.destination();
        publish(new MessageEnvelope(message.id(), destination, message.payload(),
                message.headers(), message.occurredAt()));
    }

    /// Publishes an envelope and dispatches it to local consumers.
    public void publish(MessageEnvelope message) {
        synchronized (history) {
            if (history.size() >= MAX_HISTORY) {
                history.removeFirst();
            }
            history.addLast(message);
        }
        LOG.debug("Local message [{}] to [{}]", message.messageId(), message.destination());
        bus.dispatch(message);
    }

    /// Snapshot of the retained messages.
    public List<MessageEnvelope> history() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /// Clears the retained messages.
    public void clear() {
        synchronized (history) {
            history.clear();
        }
    }
}
