package io.github.foundry4j.forge.messaging;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the message codec.
class MessageFactoryTest {

    private final MessageFactory factory = new MessageFactory(JsonMapper.builder().build());

    @Test
    void createsJsonPayloadsWithGeneratedIds() {
        var message = factory.create("order.created", "orders", new Payload("A-1", 2),
                Map.of("tenant", "acme"));

        assertThat(message.id()).isNotBlank();
        assertThat(message.type()).isEqualTo("order.created");
        assertThat(message.destination()).isEqualTo("orders");
        assertThat(message.payload()).contains("\"orderNo\":\"A-1\"").contains("\"quantity\":2");
        assertThat(message.headers()).containsEntry("tenant", "acme");
        assertThat(message.occurredAt()).isNotNull();
    }

    @Test
    void keepsStringPayloadsUntouched() {
        assertThat(factory.create("ping", "", "{}", Map.of()).payload()).isEqualTo("{}");
    }

    @Test
    void encodesAndDecodesMessages() {
        var original = factory.create("order.paid", "orders", new Payload("B-2", 1),
                Map.of("traceId", "t-1"));

        var decoded = factory.decode(factory.encode(original));

        assertThat(decoded.id()).isEqualTo(original.id());
        assertThat(decoded.type()).isEqualTo(original.type());
        assertThat(decoded.destination()).isEqualTo("orders");
        assertThat(decoded.headers()).containsEntry("traceId", "t-1");
        assertThat(decoded.occurredAt()).isEqualTo(original.occurredAt());
    }

    @Test
    void roundTripsHeaders() {
        assertThat(factory.decodeHeaders(factory.encodeHeaders(Map.of("a", "1")))).containsEntry("a", "1");
        assertThat(factory.decodeHeaders(null)).isEmpty();
        assertThat(factory.decodeHeaders(" ")).isEmpty();
    }

    /// Payload used by the codec tests.
    record Payload(String orderNo, int quantity) {
    }
}
