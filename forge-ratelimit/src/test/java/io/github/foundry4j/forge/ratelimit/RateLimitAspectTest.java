package io.github.foundry4j.forge.ratelimit;

import io.github.foundry4j.forge.core.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for the rate limiting aspect.
class RateLimitAspectTest {

    @Test
    void allowsCallsUpToTheLimitThenRejects() {
        SampleService service = proxy(new RateLimitAspect(new InMemoryRateLimiter()));

        assertThat(service.ping()).isEqualTo("pong");
        assertThat(service.ping()).isEqualTo("pong");
        assertThatThrownBy(service::ping).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void isolatesClientsThroughTheSubjectResolver() {
        var limiter = new InMemoryRateLimiter();
        var resolver = (RateLimitSubjectResolver) (strategy, method, target, args) -> (String) args[0];
        SampleService service = proxy(new RateLimitAspect(limiter, resolver));

        assertThat(service.perClient("alice")).isEqualTo("alice");
        assertThatThrownBy(() -> service.perClient("alice")).isInstanceOf(TooManyRequestsException.class);
        assertThat(service.perClient("bob")).isEqualTo("bob");
    }

    @Test
    void supportsClassLevelAnnotations() {
        var factory = new AspectJProxyFactory(new GlobalService());
        factory.addAspect(new RateLimitAspect(new InMemoryRateLimiter()));
        GlobalService service = factory.getProxy();

        assertThat(service.ping()).isEqualTo("pong");
        assertThatThrownBy(service::ping).isInstanceOf(TooManyRequestsException.class);
    }

    private static SampleService proxy(RateLimitAspect aspect) {
        var factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    /// Method level annotations.
    static class SampleService {

        @RateLimit(permits = 2, windowSeconds = 60, key = KeyStrategy.GLOBAL)
        public String ping() {
            return "pong";
        }

        @RateLimit(permits = 1, windowSeconds = 60, key = KeyStrategy.IP)
        public String perClient(String client) {
            return client;
        }
    }

    /// Type level annotation.
    @RateLimit(permits = 1, windowSeconds = 60, key = KeyStrategy.GLOBAL)
    static class GlobalService {

        public String ping() {
            return "pong";
        }
    }
}
