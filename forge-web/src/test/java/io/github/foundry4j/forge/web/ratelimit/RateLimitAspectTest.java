package io.github.foundry4j.forge.web.ratelimit;

import io.github.foundry4j.forge.cache.store.LocalKeyValueStore;
import io.github.foundry4j.forge.core.exception.TooManyRequestsException;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for the fixed window rate limiter.
class RateLimitAspectTest {

    private SampleService service;

    @BeforeEach
    void setUp() {
        var factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(new RateLimitAspect(new LocalKeyValueStore(100), new ExpressionEvaluator()));
        service = factory.getProxy();
    }

    @Test
    void allowsCallsUpToTheLimitThenRejects() {
        assertThat(service.ping()).isEqualTo("pong");
        assertThat(service.ping()).isEqualTo("pong");

        assertThatThrownBy(service::ping)
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(exception -> assertThat(
                        ((TooManyRequestsException) exception).retryAfterSeconds()).isPositive());
    }

    @Test
    void keepsOneCounterPerExpressionValue() {
        assertThat(service.perUser("alice")).isEqualTo("alice");
        assertThat(service.perUser("bob")).isEqualTo("bob");
        assertThatThrownBy(() -> service.perUser("alice")).isInstanceOf(TooManyRequestsException.class);
    }

    /// Service used to exercise the aspect through a real AOP proxy.
    static class SampleService {

        @RateLimit(name = "ping", permits = 2, windowSeconds = 60)
        public String ping() {
            return "pong";
        }

        @RateLimit(name = "per-user", permits = 1, windowSeconds = 60, key = "#user")
        public String perUser(String user) {
            return user;
        }
    }
}
