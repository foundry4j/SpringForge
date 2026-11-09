package io.github.foundry4j.forge.resilience.autoconfigure;

import io.github.foundry4j.forge.resilience.FaultToleranceExecutor;
import io.github.foundry4j.forge.resilience.FaultTolerantAspect;
import io.github.foundry4j.forge.resilience.ForgeResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Wires the resilience registries, executor and aspect.
@AutoConfiguration
@ConditionalOnProperty(prefix = "forge.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ForgeResilienceProperties.class)
public class ForgeResilienceAutoConfiguration {

    /// Circuit breaker registry configured from the global defaults.
    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry forgeCircuitBreakerRegistry(ForgeResilienceProperties properties) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(properties.slidingWindowSize())
                .failureRateThreshold(properties.failureRateThreshold())
                .waitDurationInOpenState(properties.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(properties.permittedCallsInHalfOpenState())
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    /// Retry registry configured from the global defaults.
    @Bean
    @ConditionalOnMissingBean
    public RetryRegistry forgeRetryRegistry(ForgeResilienceProperties properties) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.maxAttempts())
                .waitDuration(properties.retryWaitDuration())
                .build();
        return RetryRegistry.of(config);
    }

    /// Programmatic facade over both registries.
    @Bean
    @ConditionalOnMissingBean
    public FaultToleranceExecutor forgeFaultToleranceExecutor(CircuitBreakerRegistry circuitBreakers,
                                                              RetryRegistry retries) {
        return new FaultToleranceExecutor(circuitBreakers, retries);
    }

    /// Annotation driven fault tolerance.
    @Bean
    @ConditionalOnMissingBean
    public FaultTolerantAspect forgeFaultTolerantAspect(CircuitBreakerRegistry circuitBreakers,
                                                        RetryRegistry retries) {
        return new FaultTolerantAspect(circuitBreakers, retries);
    }
}
