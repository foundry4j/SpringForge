package io.github.foundry4j.forge.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the annotation driven fault tolerance.
class FaultTolerantAspectTest {

    private SampleService service;

    @BeforeEach
    void setUp() {
        var retries = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(1))
                .build());
        var factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(new FaultTolerantAspect(CircuitBreakerRegistry.ofDefaults(), retries));
        service = factory.getProxy();
    }

    @Test
    void invokesFallbackWithCause() {
        assertThat(service.withCause()).isEqualTo("fallback:IllegalStateException");
    }

    @Test
    void invokesFallbackWithOriginalArguments() {
        assertThat(service.withArgument("abc")).isEqualTo("fallback-abc");
    }

    @Test
    void returnsValueOnSuccess() {
        assertThat(service.success()).isEqualTo("ok");
    }

    /// Service used to exercise the aspect through a real AOP proxy.
    static class SampleService {

        @FaultTolerant(name = "with-cause", fallback = "fallbackWithCause")
        public String withCause() {
            throw new IllegalStateException("boom");
        }

        private String fallbackWithCause(Throwable cause) {
            return "fallback:" + cause.getClass().getSimpleName();
        }

        @FaultTolerant(name = "with-argument", fallback = "fallbackWithArgument")
        public String withArgument(String value) {
            throw new IllegalStateException("boom");
        }

        private String fallbackWithArgument(String value) {
            return "fallback-" + value;
        }

        @FaultTolerant(name = "success")
        public String success() {
            return "ok";
        }
    }
}
