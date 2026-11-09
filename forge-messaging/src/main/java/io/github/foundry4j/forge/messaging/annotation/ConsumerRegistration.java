package io.github.foundry4j.forge.messaging.annotation;

/// Resolved subscription of a [io.github.foundry4j.forge.messaging.MessageConsumer].
///
/// @param topic       destination to subscribe
/// @param payloadType type used to deserialize the payload
public record ConsumerRegistration(String topic, Class<?> payloadType) {
}
