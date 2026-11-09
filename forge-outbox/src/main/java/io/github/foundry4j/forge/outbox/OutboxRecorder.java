package io.github.foundry4j.forge.outbox;

import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.messaging.ForgeMessage;
import io.github.foundry4j.forge.messaging.MessageFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/// Records domain events into the outbox.
///
/// Recording is bound to the surrounding transaction ([Propagation#MANDATORY]): the event
/// is committed together with the business change, so a crash can never lose a message
/// and a client retry can never publish it twice.
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxRecorder {

    private final OutboxStore store;
    private final MessageFactory factory;

    /// Creates the recorder.
    public OutboxRecorder(OutboxStore store, MessageFactory factory) {
        this.store = store;
        this.factory = factory;
    }

    /// Records an event without an explicit destination.
    public String record(String type, Object payload) {
        return record(type, "", payload, Map.of());
    }

    /// Records an event and returns the generated message identifier.
    public String record(String type, String destination, Object payload, Map<String, String> headers) {
        ForgeMessage message = factory.create(type, destination, payload, enrich(headers));
        enqueue(message);
        return message.id();
    }

    /// Records an already built message, for example one received from another service.
    public String record(ForgeMessage message) {
        enqueue(message);
        return message.id();
    }

    private void enqueue(ForgeMessage message) {
        store.enqueue(
                message.id(),
                message.type(),
                message.destination(),
                message.payload(),
                factory.encodeHeaders(message.headers()),
                ForgeContext.tenantId().orElse(null),
                message.occurredAt());
    }

    private Map<String, String> enrich(Map<String, String> headers) {
        Map<String, String> merged = new HashMap<>(headers == null ? Map.of() : headers);
        String traceId = ForgeContext.traceId();
        if (!"-".equals(traceId)) {
            merged.putIfAbsent("traceId", traceId);
        }
        ForgeContext.tenantId().ifPresent(tenant -> merged.putIfAbsent("tenantId", tenant));
        return merged;
    }
}
