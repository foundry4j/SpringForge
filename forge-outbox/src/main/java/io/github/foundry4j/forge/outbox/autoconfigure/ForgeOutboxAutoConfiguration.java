package io.github.foundry4j.forge.outbox.autoconfigure;

import io.github.foundry4j.forge.data.autoconfigure.ForgeDataAutoConfiguration;
import io.github.foundry4j.forge.messaging.MessageFactory;
import io.github.foundry4j.forge.messaging.MessagePublisher;
import io.github.foundry4j.forge.messaging.autoconfigure.ForgeMessagingAutoConfiguration;
import io.github.foundry4j.forge.outbox.ForgeOutboxModule;
import io.github.foundry4j.forge.outbox.JpaOutboxStore;
import io.github.foundry4j.forge.outbox.LoggingOutboxTransport;
import io.github.foundry4j.forge.outbox.MessagingOutboxTransport;
import io.github.foundry4j.forge.outbox.OutboxProperties;
import io.github.foundry4j.forge.outbox.OutboxRecorder;
import io.github.foundry4j.forge.outbox.OutboxRelay;
import io.github.foundry4j.forge.outbox.OutboxRepository;
import io.github.foundry4j.forge.outbox.OutboxStore;
import io.github.foundry4j.forge.outbox.OutboxTransport;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/// Wires the transactional outbox and its background relay.
@AutoConfiguration(after = {ForgeDataAutoConfiguration.class, ForgeMessagingAutoConfiguration.class})
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(prefix = "forge.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OutboxProperties.class)
@EntityScan(basePackageClasses = ForgeOutboxModule.class)
@EnableJpaRepositories(basePackageClasses = ForgeOutboxModule.class)
public class ForgeOutboxAutoConfiguration {

    /// Message factory shared with the messaging layer.
    @Bean
    @ConditionalOnMissingBean
    public MessageFactory forgeOutboxMessageFactory(ObjectProvider<ObjectMapper> objectMapper) {
        return new MessageFactory(objectMapper.getIfAvailable(() -> JsonMapper.builder().build()));
    }

    /// JPA backed outbox storage.
    @Bean
    @ConditionalOnMissingBean
    public OutboxStore forgeOutboxStore(OutboxRepository repository, OutboxProperties properties) {
        return new JpaOutboxStore(repository, properties);
    }

    /// Service used by business code to append events to the outbox.
    @Bean
    @ConditionalOnMissingBean
    public OutboxRecorder forgeOutboxRecorder(OutboxStore store, MessageFactory factory) {
        return new OutboxRecorder(store, factory);
    }

    /// Delivery adapter, using the broker when one is configured.
    @Bean
    @ConditionalOnMissingBean
    public OutboxTransport forgeOutboxTransport(ObjectProvider<MessagePublisher> publisher) {
        return publisher.getIfAvailable() instanceof MessagePublisher available
                ? new MessagingOutboxTransport(available)
                : new LoggingOutboxTransport();
    }

    /// Starts the relay when `forge.outbox.relay-enabled` is not disabled.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "forge.outbox", name = "relay-enabled", havingValue = "true",
            matchIfMissing = true)
    @EnableScheduling
    static class RelayConfiguration implements SchedulingConfigurer {

        private final OutboxRelay relay;
        private final OutboxProperties properties;

        RelayConfiguration(OutboxRelay relay, OutboxProperties properties) {
            this.relay = relay;
            this.properties = properties;
        }

        /// Event relay.
        @Bean
        @ConditionalOnMissingBean
        static OutboxRelay forgeOutboxRelay(OutboxStore store, OutboxTransport transport,
                                            MessageFactory factory, OutboxProperties properties) {
            return new OutboxRelay(store, transport, factory, properties);
        }

        /// Registers the fixed delay task driven by configuration.
        @Override
        public void configureTasks(ScheduledTaskRegistrar registrar) {
            registrar.addFixedDelayTask(new FixedDelayTask(
                    relay::relayOnce, properties.pollInterval(), properties.pollInterval()));
        }
    }
}
