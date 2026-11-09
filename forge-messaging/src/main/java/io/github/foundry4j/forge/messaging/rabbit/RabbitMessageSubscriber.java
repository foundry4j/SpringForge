package io.github.foundry4j.forge.messaging.rabbit;

import io.github.foundry4j.forge.messaging.MessageEnvelope;
import io.github.foundry4j.forge.messaging.MessageSubscriber;
import io.github.foundry4j.forge.messaging.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.DisposableBean;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/// RabbitMQ consumer with manual acknowledgement.
///
/// Every subscription declares a durable queue named after the topic, binds it to the
/// configured topic exchange and starts its own listener container. A message is
/// acknowledged after the consumer returns and rejected without requeue when it
/// throws, so one poison message cannot spin forever.
public class RabbitMessageSubscriber implements MessageSubscriber, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMessageSubscriber.class);

    private final ConnectionFactory connectionFactory;
    private final MessagingProperties properties;
    private final AmqpAdmin amqpAdmin;
    private final List<SimpleMessageListenerContainer> containers = new ArrayList<>();

    /// Creates the subscriber.
    public RabbitMessageSubscriber(ConnectionFactory connectionFactory, MessagingProperties properties,
                                   AmqpAdmin amqpAdmin) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
        this.amqpAdmin = amqpAdmin;
    }

    @Override
    public CompletionStage<Void> subscribe(String topic, Consumer<MessageEnvelope> handler) {
        declareTopology(topic);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(topic);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
            try {
                handler.accept(toEnvelope(topic, message));
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (RuntimeException ex) {
                LOG.warn("RabbitMQ consumer failed for [{}]", topic, ex);
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            }
        });
        container.afterPropertiesSet();
        container.start();
        containers.add(container);
        return CompletableFuture.completedFuture(null);
    }

    /// Stops every container.
    @Override
    public void destroy() {
        containers.forEach(SimpleMessageListenerContainer::stop);
        containers.clear();
    }

    private void declareTopology(String topic) {
        if (amqpAdmin == null) {
            return;
        }
        TopicExchange exchange = new TopicExchange(properties.exchange(), true, false);
        Queue queue = new Queue(topic, true);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(topic));
    }

    private MessageEnvelope toEnvelope(String topic, Message message) {
        Map<String, String> headers = new LinkedHashMap<>();
        message.getMessageProperties().getHeaders().forEach((key, value) ->
                headers.put(key, value == null ? "" : value.toString()));
        Instant occurredAt = message.getMessageProperties().getTimestamp() == null
                ? Instant.now()
                : message.getMessageProperties().getTimestamp().toInstant();
        return new MessageEnvelope(message.getMessageProperties().getMessageId(), topic,
                new String(message.getBody(), StandardCharsets.UTF_8), headers, occurredAt);
    }
}
