package io.github.foundry4j.forge.ratelimit.autoconfigure;

import io.github.foundry4j.forge.ratelimit.InMemoryRateLimiter;
import io.github.foundry4j.forge.ratelimit.RateLimiter;
import io.github.foundry4j.forge.ratelimit.RedisRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/// Verifies that the provider property switches between the local and the shared limiter.
class ForgeRateLimitAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ForgeRedisRateLimitAutoConfiguration.class,
                    ForgeRateLimitAutoConfiguration.class));

    @Test
    void defaultsToTheInMemoryLimiter() {
        runner.run(context -> assertThat(context).getBean(RateLimiter.class)
                .isInstanceOf(InMemoryRateLimiter.class));
    }

    @Test
    void usesRedisWhenRequested() {
        runner.withPropertyValues("forge.ratelimit.provider=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> assertThat(context).getBean(RateLimiter.class)
                        .isInstanceOf(RedisRateLimiter.class));
    }
}
