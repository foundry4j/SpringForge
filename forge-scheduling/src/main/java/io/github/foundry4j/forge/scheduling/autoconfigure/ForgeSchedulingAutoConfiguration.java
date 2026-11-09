package io.github.foundry4j.forge.scheduling.autoconfigure;

import io.github.foundry4j.forge.scheduling.ForgeJobFactory;
import io.github.foundry4j.forge.scheduling.ForgeScheduledRegistrar;
import io.github.foundry4j.forge.scheduling.ForgeScheduler;
import io.github.foundry4j.forge.scheduling.ForgeSchedulingProperties;
import org.quartz.Scheduler;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/// Wires the Quartz scheduler facade and the annotation driven job registrar.
@AutoConfiguration(after = QuartzAutoConfiguration.class)
@ConditionalOnClass({Scheduler.class, SchedulerFactoryBean.class})
@ConditionalOnProperty(prefix = "forge.scheduling", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(ForgeSchedulingProperties.class)
public class ForgeSchedulingAutoConfiguration {

    /// Creates jobs through the Spring bean factory so they support injection.
    @Bean
    @ConditionalOnMissingBean
    public ForgeJobFactory forgeJobFactory(AutowireCapableBeanFactory beanFactory) {
        return new ForgeJobFactory(beanFactory);
    }

    /// Applies the job factory and graceful shutdown to the Quartz scheduler.
    @Bean
    public SchedulerFactoryBeanCustomizer forgeQuartzCustomizer(ForgeJobFactory jobFactory) {
        return factory -> {
            factory.setJobFactory(jobFactory);
            factory.setWaitForJobsToCompleteOnShutdown(true);
            factory.setOverwriteExistingJobs(true);
        };
    }

    /// Application facing scheduler facade.
    @Bean
    @ConditionalOnMissingBean
    public ForgeScheduler forgeScheduler(Scheduler scheduler, ForgeSchedulingProperties properties) {
        return new ForgeScheduler(scheduler, properties);
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
