package io.github.foundry4j.forge.messaging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares a broker neutral message consumer.
///
/// Implement [EnvelopeMessageHandler] and add this annotation, and the starter
/// subscribes the bean during startup:
///
/// ```java
/// @MessageConsumer(topic = "order.created", payloadType = OrderCreated.class)
/// public class OrderCreatedConsumer implements EnvelopeMessageHandler<OrderCreated> {
///
///     @Override
///     public void onMessage(OrderCreated payload, MessageEnvelope envelope) {
///         // idempotency can use envelope.messageId()
///     }
/// }
/// ```
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MessageConsumer {

    /// Destination or topic to subscribe.
    String topic();

    /// Type used to deserialize [MessageEnvelope#payload()]; `String` keeps the raw JSON.
    Class<?> payloadType() default String.class;
}
