package io.github.foundry4j.forge.outbox;

import io.github.foundry4j.forge.messaging.ForgeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fallback transport used when no broker is configured.
///
/// It only writes the event to the log, which keeps the whole outbox pipeline running in
/// local development and tests without any infrastructure.
public class LoggingOutboxTransport implements OutboxTransport {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingOutboxTransport.class);

    /// {@inheritDoc}
    @Override
    public void deliver(ForgeMessage message) {
        LOG.info("Outbox event delivered to log: id={} type={} destination={} payload={}",
                message.id(), message.type(), message.destination(), message.payload());
    }
}
