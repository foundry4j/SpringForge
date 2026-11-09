package io.github.foundry4j.forge.messaging.autoconfigure;

import io.github.foundry4j.forge.messaging.MessageFactory;
import io.github.foundry4j.forge.messaging.MessageHandler;
import io.github.foundry4j.forge.messaging.MessagePublisher;
import io.github.foundry4j.forge.messaging.MessageRouter;
import io.github.foundry4j.forge.messaging.MessagingProperties;
import io.github.foundry4j.forge.messaging.kafka.KafkaMessagePublisher;
import io.github.foundry4j.forge.messaging.rabbit.RabbitMessagePublisher;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/// Wires the broker independent messaging abstractions.
@AutoConfiguration
@ConditionalOnProperty(prefix = "forge.messaging", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MessagingProperties.class)
public class ForgeMessagingAutoConfiguration {

    /// Shared message factory.
    @Bean
    @ConditionalOnMissingBean
    public MessageFactory forgeMessageFactory(ObjectProvider<ObjectMapper> objectMapper) {
        return new MessageFactory(objectMapper.getIfAvailable(() -> JsonMapper.builder().build()));
    }

    /// Routes consumed messages to the registered handlers.
    @Bean
    @ConditionalOnMissingBean
    public MessageRouter forgeMessageRouter(MessageFactory factory, ObjectProvider<MessageHandler> handlers) {
        List<MessageHandler> available = handlers.orderedStream().toList();
        return new MessageRouter(factory, available);
    }

    /// RabbitMQ specific beans.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RabbitTemplate.class)
    @ConditionalOnProperty(prefix = "forge.messaging", name = "broker", havingValue = "rabbitmq")
    static class RabbitConfiguration {

        /// RabbitMQ publisher.
        @Bean
        @ConditionalOnMissingBean(MessagePublisher.class)
        RabbitMessagePublisher forgeRabbitMessagePublisher(RabbitTemplate rabbitTemplate,
                                                           MessagingProperties properties,
                                                           MessageFactory factory) {
            return new RabbitMessagePublisher(rabbitTemplate, properties, factory);
        }

        /// Declares the exchange, queue and binding configured through properties.
        @Bean
        @ConditionalOnExpression("'${forge.messaging.queue:}'.length() > 0")
        Declarables forgeRabbitTopology(MessagingProperties properties) {
            TopicExchange exchange = new TopicExchange(properties.exchange(), true, false);
            Queue queue = new Queue(properties.queue(), true);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(properties.routingKey());
            return new Declarables(exchange, queue, binding);
        }
    }

    /// Kafka specific beans.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaTemplate.class)
    @ConditionalOnProperty(prefix = "forge.messaging", name = "broker", havingValue = "kafka")
    static class KafkaConfiguration {

        /// Kafka publisher.
        @Bean
        @ConditionalOnMissingBean(MessagePublisher.class)
        KafkaMessagePublisher forgeKafkaMessagePublisher(KafkaTemplate<String, String> kafkaTemplate,
                                                         MessagingProperties properties,
                                                         MessageFactory factory) {
            return new KafkaMessagePublisher(kafkaTemplate, properties, factory);
        }
    }
}
