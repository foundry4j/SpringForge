package io.github.foundry4j.forge.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Routes a decoded message to the [MessageHandler] registered for its type.
///
/// Broker listeners only need to call [dispatch]; the router takes care of decoding,
/// handler lookup and error reporting.
public final class MessageRouter {

    private static final Logger LOG = LoggerFactory.getLogger(MessageRouter.class);

    private final MessageFactory factory;
    private final Map<String, MessageHandler> handlers;

    /// Creates the router from all handler beans of the application context.
    public MessageRouter(MessageFactory factory, List<MessageHandler> handlers) {
        this.factory = factory;
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(MessageHandler::type, Function.identity(), (first, _) -> first));
    }

    /// Decodes and dispatches a raw broker payload.
    ///
    /// @throws IllegalArgumentException when no handler is registered for the message type
    public void dispatch(String json) {
        ForgeMessage message = factory.decode(json);
        MessageHandler handler = handlers.get(message.type());
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for message type [%s]".formatted(message.type()));
        }
        LOG.debug("Dispatching message [{}] of type [{}]", message.id(), message.type());
        handler.handle(message);
    }

    /// Types this router can handle.
    public Set<String> types() {
        return handlers.keySet();
    }
}
