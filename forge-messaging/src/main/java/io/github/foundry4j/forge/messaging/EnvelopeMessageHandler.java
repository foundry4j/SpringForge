package io.github.foundry4j.forge.messaging;

/// Handles a deserialized message with access to its envelope.
///
/// @param <T> payload type, `String` receives the raw JSON
public interface EnvelopeMessageHandler<T> {

    /// Processes one message.
    ///
    /// An exception is logged by the local bus and turns into a `nack` on RabbitMQ, so
    /// a poison message does not block the queue forever.
    void onMessage(T payload, MessageEnvelope envelope);
}
