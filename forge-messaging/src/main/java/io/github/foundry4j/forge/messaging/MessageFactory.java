package io.github.foundry4j.forge.messaging;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

/// Creates and decodes [ForgeMessage] instances.
///
/// The factory centralizes JSON serialization so RabbitMQ and Kafka share the exact
/// same wire format.
public class MessageFactory {

    private final ObjectMapper objectMapper;

    /// Creates the factory with the application's Jackson mapper.
    public MessageFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /// Serializes `payload` and returns a message ready to be published.
    public ForgeMessage create(String type, String destination, Object payload, Map<String, String> headers) {
        String json = payload instanceof String text ? text : objectMapper.writeValueAsString(payload);
        return new ForgeMessage(null, type, destination, json, headers, Instant.now());
    }

    /// Serializes a message for the transport.
    public String encode(ForgeMessage message) {
        return objectMapper.writeValueAsString(message);
    }

    /// Parses a message received from a transport.
    public ForgeMessage decode(String json) {
        return objectMapper.readValue(json, ForgeMessage.class);
    }

    /// Serializes transport headers so they can be stored by the outbox.
    public String encodeHeaders(Map<String, String> headers) {
        return objectMapper.writeValueAsString(headers == null ? Map.of() : headers);
    }

    /// Restores transport headers persisted by the outbox.
    public Map<String, String> decodeHeaders(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
        });
    }
}
