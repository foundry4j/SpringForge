package io.github.foundry4j.forge.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Global defaults for the circuit breaker and retry policies.
///
/// Per name tuning is possible by declaring a custom
/// [io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry] bean.
///
/// @param enabled                       whether the resilience beans are registered
/// @param slidingWindowSize             number of calls observed by the circuit breaker
/// @param failureRateThreshold          percentage of failures that opens the circuit
/// @param waitDurationInOpenState       how long the circuit stays open
/// @param permittedCallsInHalfOpenState trials allowed while half open
/// @param maxAttempts                   total attempts of the retry policy
/// @param retryWaitDuration             pause between two retries
@ConfigurationProperties("forge.resilience")
public record ForgeResilienceProperties(
        Boolean enabled,
        Integer slidingWindowSize,
        Float failureRateThreshold,
        Duration waitDurationInOpenState,
        Integer permittedCallsInHalfOpenState,
        Integer maxAttempts,
        Duration retryWaitDuration
) {

    /// Applies conservative defaults.
    public ForgeResilienceProperties {
        enabled = enabled == null || enabled;
        slidingWindowSize = slidingWindowSize == null || slidingWindowSize < 2 ? 20 : slidingWindowSize;
        failureRateThreshold = failureRateThreshold == null ? 50.0F : failureRateThreshold;
        waitDurationInOpenState = waitDurationInOpenState == null ? Duration.ofSeconds(10) : waitDurationInOpenState;
        permittedCallsInHalfOpenState = permittedCallsInHalfOpenState == null ? 3 : permittedCallsInHalfOpenState;
        maxAttempts = maxAttempts == null || maxAttempts < 1 ? 3 : maxAttempts;
        retryWaitDuration = retryWaitDuration == null ? Duration.ofMillis(200) : retryWaitDuration;
    }
}
