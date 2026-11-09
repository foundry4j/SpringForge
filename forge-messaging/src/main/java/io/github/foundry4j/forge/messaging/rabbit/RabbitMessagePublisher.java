package io.github.foundry4j.forge.messaging.rabbit;

import io.github.foundry4j.forge.messaging.ForgeMessage;
import io.github.foundry4j.forge.messaging.MessageFactory;
import io.github.foundry4j.forge.messaging.MessagePublisher;
import io.github.foundry4j.forge.messaging.MessagingProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/// Publishes messages through RabbitMQ.
///
/// The message is sent as a JSON string so consumers can decode it with
/// [MessageFactory] regardless of their object model.
public final class RabbitMessagePublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;
    private final MessageFactory factory;

    /// Creates the publisher.
    public RabbitMessagePublisher(RabbitTemplate rabbitTemplate,
                                  MessagingProperties properties,
                                  MessageFactory factory) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.factory = factory;
    }

    @Override
    public void publish(ForgeMessage message) {
        String routingKey = message.destination().isBlank() ? properties.routingKey() : message.destination();
        rabbitTemplate.convertAndSend(properties.exchange(), routingKey, factory.encode(message));
    }
}
