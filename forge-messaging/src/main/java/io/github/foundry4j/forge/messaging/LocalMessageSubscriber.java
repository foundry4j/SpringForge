package io.github.foundry4j.forge.messaging;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/// [MessageSubscriber] backed by [LocalMessageBus].
public class LocalMessageSubscriber implements MessageSubscriber {

    private final LocalMessageBus bus;

    /// Creates the subscriber.
    public LocalMessageSubscriber(LocalMessageBus bus) {
        this.bus = bus;
    }

    @Override
    public CompletionStage<Void> subscribe(String topic, Consumer<MessageEnvelope> handler) {
        bus.subscribe(topic, handler);
        return CompletableFuture.completedFuture(null);
    }
}
