package io.github.foundry4j.forge.resilience;

import io.github.foundry4j.forge.core.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for the programmatic fault tolerance executor.
class FaultToleranceExecutorTest {

    private final CircuitBreakerRegistry circuitBreakers = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                    .slidingWindowSize(4)
                    .minimumNumberOfCalls(4)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofMillis(10))
                    .build());
    private final RetryRegistry retries = RetryRegistry.of(
            RetryConfig.custom()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofMillis(1))
                    .build());
    private final FaultToleranceExecutor executor = new FaultToleranceExecutor(circuitBreakers, retries);

    @Test
    void returnsActionResult() {
        assertThat(executor.execute("ok", () -> "value", "fallback")).isEqualTo("value");
    }

    @Test
    void usesFallbackAfterAllRetries() {
        var attempts = new AtomicInteger();
        String result = executor.execute("failing", () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("down");
        }, "fallback");

        assertThat(result).isEqualTo("fallback");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void usesConstantFallbackValue() {
        assertThat(executor.execute("value", () -> {
            throw new IllegalStateException("down");
        }, 42)).isEqualTo(42);
    }

    @Test
    void rethrowsWhenNoFallbackIsConfigured() {
        assertThatThrownBy(() -> executor.execute("no-fallback", () -> {
            throw new IllegalStateException("down");
        }, (String) null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reportsServiceUnavailableWhenAnOpenCircuitHasNoFallback() {
        CircuitBreakerRegistry longOpenRegistry = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom()
                        .waitDurationInOpenState(Duration.ofMinutes(1))
                        .build());
        longOpenRegistry.circuitBreaker("open").transitionToOpenState();
        var openExecutor = new FaultToleranceExecutor(longOpenRegistry, retries);

        assertThatThrownBy(() -> openExecutor.<String>execute("open", () -> "never", (Supplier<String>) null))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
