package io.github.foundry4j.forge.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/// In-process message bus used when no broker is configured.
///
/// Dispatch is synchronous on the publishing thread, which keeps local development
/// and tests deterministic. A failing consumer is logged and does not affect the
/// other consumers of the same destination.
public class LocalMessageBus {

    private static final Logger LOG = LoggerFactory.getLogger(LocalMessageBus.class);

    private final Map<String, List<Consumer<MessageEnvelope>>> subscribers = new ConcurrentHashMap<>();

    /// Registers a consumer.
    public void subscribe(String topic, Consumer<MessageEnvelope> handler) {
        subscribers.computeIfAbsent(topic, _ -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /// Number of consumers of a destination, mainly useful in tests.
    public int subscriberCount(String topic) {
        List<Consumer<MessageEnvelope>> handlers = subscribers.get(topic);
        return handlers == null ? 0 : handlers.size();
    }

    /// Dispatches a message to every consumer of its destination.
    public void dispatch(MessageEnvelope envelope) {
        List<Consumer<MessageEnvelope>> handlers = subscribers.get(envelope.destination());
        if (handlers == null || handlers.isEmpty()) {
            return;
        }
        for (Consumer<MessageEnvelope> handler : handlers) {
            try {
                handler.accept(envelope);
            } catch (RuntimeException ex) {
                LOG.warn("Local consumer failed for destination [{}] and message [{}]",
                        envelope.destination(), envelope.messageId(), ex);
            }
        }
    }
}
