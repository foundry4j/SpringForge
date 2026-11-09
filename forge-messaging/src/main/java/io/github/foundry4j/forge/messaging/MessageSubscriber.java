package io.github.foundry4j.forge.messaging;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/// Subscribes consumers to a broker destination.
///
/// The local implementation is used when no broker is configured; a broker module
/// replaces it, so application consumers stay transport neutral.
public interface MessageSubscriber {

    /// Registers a consumer.
    ///
    /// @param topic   destination to listen on
    /// @param handler envelope consumer
    /// @return completion of the subscription, a failed stage reports an unreachable broker
    CompletionStage<Void> subscribe(String topic, Consumer<MessageEnvelope> handler);
}
