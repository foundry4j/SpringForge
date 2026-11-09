package io.github.foundry4j.forge.outbox;

import io.github.foundry4j.forge.messaging.ForgeMessage;
import io.github.foundry4j.forge.messaging.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/// Background relay that moves committed outbox events to the broker.
///
/// Every run performs three steps: abandoned events are recovered, a batch of pending
/// events is claimed, and each claimed event is delivered. Delivery failures are recorded
/// with an exponential backoff, so the platform keeps retrying without operator help.
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore store;
    private final OutboxTransport transport;
    private final MessageFactory factory;
    private final OutboxProperties properties;

    /// Creates the relay.
    public OutboxRelay(OutboxStore store, OutboxTransport transport,
                       MessageFactory factory, OutboxProperties properties) {
        this.store = store;
        this.transport = transport;
        this.factory = factory;
        this.properties = properties;
    }

    /// Performs a single relay pass.
    public void relayOnce() {
        try {
            int recovered = store.recoverStuck(properties.stuckTimeout());
            if (recovered > 0) {
                LOG.warn("Recovered {} abandoned outbox events", recovered);
            }
            int purged = store.purge(properties.retention());
            if (purged > 0) {
                LOG.debug("Purged {} delivered outbox events", purged);
            }
            List<OutboxEvent> events = store.claim(properties.batchSize());
            if (events.isEmpty()) {
                return;
            }
            LOG.debug("Relaying {} outbox events", events.size());
            for (OutboxEvent event : events) {
                deliver(event);
            }
        } catch (RuntimeException ex) {
            LOG.warn("Outbox relay pass failed", ex);
        }
    }

    private void deliver(OutboxEvent event) {
        ForgeMessage message = new ForgeMessage(
                event.getMessageId(),
                event.getType(),
                event.getDestination(),
                event.getPayload(),
                factory.decodeHeaders(event.getHeaders()),
                event.getOccurredAt());
        try {
            transport.deliver(message);
            store.markSent(event.getId(), Instant.now());
        } catch (RuntimeException ex) {
            LOG.warn("Outbox delivery failed for event {} (type {})", event.getId(), event.getType(), ex);
            store.markFailed(event.getId(), ex.getMessage(), Instant.now());
        }
    }
}
