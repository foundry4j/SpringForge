package io.github.foundry4j.forge.messaging;

import io.github.foundry4j.forge.core.support.Ids;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// Broker neutral envelope of a consumed message.
///
/// The envelope travels unchanged through the transport. `messageId` enables
/// idempotent consumers, `destination` is the routing key or topic and `headers`
/// carry string values only so that every broker can transport them.
///
/// @param messageId   unique identifier, generated when missing
/// @param destination routing key or topic
/// @param payload     JSON text of the body
/// @param headers     transport headers
/// @param occurredAt  creation time
public record MessageEnvelope(String messageId,
                              String destination,
                              String payload,
                              Map<String, String> headers,
                              Instant occurredAt) {

    /// Normalizes optional components.
    public MessageEnvelope {
        messageId = messageId == null || messageId.isBlank() ? Ids.compactId() : messageId;
        destination = Objects.requireNonNull(destination, "destination must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    /// Creates a message with a generated identifier and the current time.
    public static MessageEnvelope of(String destination, String payload) {
        return new MessageEnvelope(null, destination, payload, Map.of(), Instant.now());
    }

    /// Creates a message with additional headers.
    public static MessageEnvelope of(String destination, String payload, Map<String, String> headers) {
        return new MessageEnvelope(null, destination, payload, headers, Instant.now());
    }

    /// Returns a copy with one additional header.
    public MessageEnvelope withHeader(String name, String value) {
        Map<String, String> merged = new HashMap<>(headers);
        merged.put(name, value);
        return new MessageEnvelope(messageId, destination, payload, merged, occurredAt);
    }
}
