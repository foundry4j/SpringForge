package io.github.foundry4j.forge.messaging;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for type based message dispatching.
class MessageRouterTest {

    private final MessageFactory factory = new MessageFactory(JsonMapper.builder().build());

    @Test
    void dispatchesToTheHandlerOfTheType() {
        var received = new AtomicReference<ForgeMessage>();
        var router = new MessageRouter(factory, List.of(handler("order.created", received)));
        var message = factory.create("order.created", "", new Payload("X"), Map.of());

        router.dispatch(factory.encode(message));

        assertThat(received.get()).isNotNull();
        assertThat(received.get().type()).isEqualTo("order.created");
        assertThat(router.types()).containsExactly("order.created");
    }

    @Test
    void rejectsUnknownTypes() {
        var router = new MessageRouter(factory, List.of());
        var message = factory.create("unknown", "", "{}", Map.of());

        assertThatThrownBy(() -> router.dispatch(factory.encode(message)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void keepsTheFirstHandlerOfADuplicatedType() {
        var first = new AtomicInteger();
        var second = new AtomicInteger();
        var router = new MessageRouter(factory, List.of(handler("dup", first), handler("dup", second)));

        router.dispatch(factory.encode(factory.create("dup", "", "{}", Map.of())));

        assertThat(first).hasValue(1);
        assertThat(second).hasValue(0);
    }

    private static MessageHandler handler(String type, AtomicReference<ForgeMessage> received) {
        return new MessageHandler() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public void handle(ForgeMessage message) {
                received.set(message);
            }
        };
    }

    private static MessageHandler handler(String type, AtomicInteger counter) {
        return new MessageHandler() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public void handle(ForgeMessage message) {
                counter.incrementAndGet();
            }
        };
    }

    /// Payload used by the router tests.
    record Payload(String id) {
    }
}
