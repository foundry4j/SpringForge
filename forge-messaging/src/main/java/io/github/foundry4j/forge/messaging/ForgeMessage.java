package io.github.foundry4j.forge.messaging;

import io.github.foundry4j.forge.core.support.Ids;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/// Broker neutral message envelope.
///
/// The payload is stored as JSON text so that producers and consumers never depend on
/// each other's classes. RabbitMQ and Kafka publishers only have to move this record.
///
/// @param id          unique message identifier, generated when missing
/// @param type        logical event type, for example `order.created`
/// @param destination routing key or topic suffix, may be blank
/// @param payload     JSON serialized body
/// @param headers     transport headers
/// @param occurredAt  creation time
public record ForgeMessage(
        String id,
        String type,
        String destination,
        String payload,
        Map<String, String> headers,
        Instant occurredAt
) {

    /// Normalizes optional components.
    public ForgeMessage {
        id = id == null || id.isBlank() ? Ids.compactId() : id;
        type = Objects.requireNonNull(type, "type must not be null");
        destination = destination == null ? "" : destination;
        payload = payload == null ? "" : payload;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
