package io.github.foundry4j.forge.scheduling.autoconfigure;

import io.github.foundry4j.forge.scheduling.ForgeScheduledRegistrar;
import io.github.foundry4j.forge.scheduling.ForgeScheduler;
import io.github.foundry4j.forge.scheduling.ForgeSchedulingProperties;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Wires the JobRunr facade and the annotation driven job registrar.
///
/// JobRunr itself is configured through its own `jobrunr.*` properties; the
/// `jobrunr.background-job-server.enabled` flag decides whether the node processes jobs.
@AutoConfiguration
@ConditionalOnClass(JobScheduler.class)
@ConditionalOnProperty(prefix = "forge.scheduling", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(ForgeSchedulingProperties.class)
public class ForgeSchedulingAutoConfiguration {

    /// Application facing scheduler facade.
    @Bean
    @ConditionalOnMissingBean
    public ForgeScheduler forgeScheduler(JobScheduler jobScheduler, StorageProvider storageProvider,
                                         ForgeSchedulingProperties properties) {
        return new ForgeScheduler(jobScheduler, storageProvider, properties);
    }

    /// Registers methods annotated with `@ForgeScheduled`.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "forge.scheduling", name = "auto-register", havingValue = "true",
            matchIfMissing = true)
    static class AnnotationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        static ForgeScheduledRegistrar forgeScheduledRegistrar(ForgeScheduler scheduler,
                                                               ForgeSchedulingProperties properties) {
            return new ForgeScheduledRegistrar(scheduler, properties);
        }
    }
}
