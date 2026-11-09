package io.github.foundry4j.forge.messaging;

import io.github.foundry4j.forge.messaging.annotation.ConsumerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/// Subscribes every bean annotated with [MessageConsumer] during startup.
///
/// The bootstrap happens after all singletons are ready, so handlers can depend on
/// any other bean without ordering surprises.
public class MessageConsumerInitializer implements SmartInitializingSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumerInitializer.class);

    private final ApplicationContext context;
    private final MessageFactory factory;
    private final MessageSubscriber subscriber;

    /// Creates the initializer.
    public MessageConsumerInitializer(ApplicationContext context, MessageFactory factory,
                                      MessageSubscriber subscriber) {
        this.context = context;
        this.factory = factory;
        this.subscriber = subscriber;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> consumers = context.getBeansWithAnnotation(MessageConsumer.class);
        consumers.forEach((name, bean) -> {
            MessageConsumer annotation = context.findAnnotationOnBean(name, MessageConsumer.class);
            if (annotation == null) {
                return;
            }
            if (!(bean instanceof EnvelopeMessageHandler<?> handler)) {
                LOG.warn("Bean [{}] is annotated with @MessageConsumer but does not implement "
                        + "EnvelopeMessageHandler", name);
                return;
            }
            ConsumerRegistration registration = new ConsumerRegistration(
                    annotation.topic(), annotation.payloadType());
            subscriber.subscribe(registration.topic(), envelope -> dispatch(handler, registration, envelope));
            LOG.info("Subscribed consumer [{}] to [{}]", name, registration.topic());
        });
    }

    private <T> void dispatch(EnvelopeMessageHandler<T> handler, ConsumerRegistration registration,
                              MessageEnvelope envelope) {
        @SuppressWarnings("unchecked")
        T payload = (T) (registration.payloadType() == String.class
                ? envelope.payload()
                : factory.decodePayload(envelope.payload(), registration.payloadType()));
        handler.onMessage(payload, envelope);
    }
}
