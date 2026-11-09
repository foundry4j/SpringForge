package io.github.foundry4j.forge.messaging;

/// Consumes messages routed by their [ForgeMessage#type()].
///
/// Every implementation is registered as a Spring bean and picked up by
/// [MessageRouter]; a type may only have one handler.
public interface MessageHandler {

    /// Message type this handler is responsible for.
    String type();

    /// Handles the message. Exceptions are propagated to the listener container so the
    /// broker can redeliver or move the message to a dead letter queue.
    void handle(ForgeMessage message);
}
