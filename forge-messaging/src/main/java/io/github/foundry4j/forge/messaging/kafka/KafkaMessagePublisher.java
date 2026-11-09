package io.github.foundry4j.forge.messaging.kafka;

import io.github.foundry4j.forge.messaging.ForgeMessage;
import io.github.foundry4j.forge.messaging.MessageFactory;
import io.github.foundry4j.forge.messaging.MessagePublisher;
import io.github.foundry4j.forge.messaging.MessagingProperties;
import org.springframework.kafka.core.KafkaTemplate;

/// Publishes messages through Apache Kafka.
///
/// The message identifier is used as the record key, which keeps the ordering of all
/// messages that belong to the same logical event.
public final class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MessagingProperties properties;
    private final MessageFactory factory;

    /// Creates the publisher.
    public KafkaMessagePublisher(KafkaTemplate<String, String> kafkaTemplate,
                                 MessagingProperties properties,
                                 MessageFactory factory) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.factory = factory;
    }

    @Override
    public void publish(ForgeMessage message) {
        String suffix = message.destination().isBlank() ? message.type() : message.destination();
        kafkaTemplate.send(properties.topicPrefix() + suffix, message.id(), factory.encode(message));
    }
}
