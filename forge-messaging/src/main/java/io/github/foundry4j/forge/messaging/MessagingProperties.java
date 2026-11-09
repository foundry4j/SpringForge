package io.github.foundry4j.forge.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the messaging layer.
///
/// @param enabled      whether forge messaging beans are registered
/// @param broker       selected broker
/// @param exchange     RabbitMQ exchange
/// @param exchangeType RabbitMQ exchange type
/// @param queue        optional queue declared at startup
/// @param routingKey   default RabbitMQ routing key
/// @param topicPrefix  prefix prepended to Kafka topic names
@ConfigurationProperties("forge.messaging")
public record MessagingProperties(
        Boolean enabled,
        Broker broker,
        String exchange,
        String exchangeType,
        String queue,
        String routingKey,
        String topicPrefix
) {

    /// Supported brokers.
    public enum Broker {
        /// Messaging is disabled.
        NONE,
        /// RabbitMQ through Spring AMQP.
        RABBITMQ,
        /// Apache Kafka through Spring Kafka.
        KAFKA
    }

    /// Applies defaults that keep the layer disabled unless explicitly enabled.
    public MessagingProperties {
        enabled = enabled != null && enabled;
        broker = broker == null ? Broker.NONE : broker;
        exchange = exchange == null || exchange.isBlank() ? "forge.events" : exchange;
        exchangeType = exchangeType == null || exchangeType.isBlank() ? "topic" : exchangeType;
        queue = queue == null ? "" : queue;
        routingKey = routingKey == null || routingKey.isBlank() ? "forge.default" : routingKey;
        topicPrefix = topicPrefix == null ? "" : topicPrefix;
    }
}
