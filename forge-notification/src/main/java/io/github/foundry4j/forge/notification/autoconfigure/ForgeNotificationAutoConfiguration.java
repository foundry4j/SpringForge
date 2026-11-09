package io.github.foundry4j.forge.notification.autoconfigure;

import io.github.foundry4j.forge.data.autoconfigure.ForgeDataAutoConfiguration;
import io.github.foundry4j.forge.notification.ForgeNotificationModule;
import io.github.foundry4j.forge.notification.NotificationProperties;
import io.github.foundry4j.forge.notification.NotificationRepository;
import io.github.foundry4j.forge.notification.NotificationSender;
import io.github.foundry4j.forge.notification.NotificationService;
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

/// Wires the notification log, the sender SPI and the background retry.
@AutoConfiguration(after = ForgeDataAutoConfiguration.class)
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(prefix = "forge.notification", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(NotificationProperties.class)
@EntityScan(basePackageClasses = ForgeNotificationModule.class)
@EnableJpaRepositories(basePackageClasses = ForgeNotificationModule.class)
public class ForgeNotificationAutoConfiguration {

    /// Notification history and delivery service.
    ///
    /// Application supplied [NotificationSender] beans replace the built-in logging sender
    /// of their channel.
    @Bean
    @ConditionalOnMissingBean
    public NotificationService forgeNotificationService(NotificationRepository repository,
                                                        ObjectProvider<NotificationSender> senders,
                                                        NotificationProperties properties) {
        return new NotificationService(repository, senders.orderedStream().toList(), properties);
    }

    /// Retries pending notifications when `forge.notification.retry-enabled` is not disabled.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "forge.notification", name = "retry-enabled", havingValue = "true",
            matchIfMissing = true)
    @EnableScheduling
    static class RetryConfiguration implements SchedulingConfigurer {

        private final NotificationService service;
        private final NotificationProperties properties;

        RetryConfiguration(NotificationService service, NotificationProperties properties) {
            this.service = service;
            this.properties = properties;
        }

        /// Registers the fixed delay task driven by configuration.
        @Override
        public void configureTasks(ScheduledTaskRegistrar registrar) {
            registrar.addFixedDelayTask(new FixedDelayTask(
                    service::dispatchPending, properties.pollInterval(), properties.pollInterval()));
        }
    }
}
