package io.github.foundry4j.forge.resilience;

import io.github.foundry4j.forge.core.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

import java.util.function.Supplier;

/// Programmatic entry point for fault tolerance.
///
/// The executor combines a circuit breaker with a retry policy. When the circuit is
/// open, or the action still fails after all retries, the fallback is used if one was
/// supplied; otherwise a [ServiceUnavailableException] is raised.
public class FaultToleranceExecutor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /// Creates the executor.
    public FaultToleranceExecutor(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    /// Executes `action` protected by the circuit breaker named `name`.
    ///
    /// @param name     circuit breaker and retry name
    /// @param action   protected action
    /// @param fallback degradation used when the action fails, may be `null`
    public <T> T execute(String name, Supplier<T> action, Supplier<T> fallback) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, action));
        try {
            return decorated.get();
        } catch (CallNotPermittedException ex) {
            if (fallback != null) {
                return fallback.get();
            }
            throw new ServiceUnavailableException("Circuit [%s] is open".formatted(name), ex);
        } catch (RuntimeException ex) {
            if (fallback != null) {
                return fallback.get();
            }
            throw ex;
        }
    }

    /// Executes `action` with the given fallback value.
    ///
    /// A `null` value is treated as "no fallback", so the original exception is rethrown
    /// exactly like in [ #execute(String, Supplier, Supplier)].
    public <T> T execute(String name, Supplier<T> action, T fallbackValue) {
        Supplier<T> fallback = fallbackValue == null ? null : () -> fallbackValue;
        return this.<T>execute(name, action, fallback);
    }

    /// Circuit breaker registry, useful for health reporting.
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return circuitBreakerRegistry;
    }

    /// Retry registry, useful for health reporting.
    public RetryRegistry retryRegistry() {
        return retryRegistry;
    }
}
