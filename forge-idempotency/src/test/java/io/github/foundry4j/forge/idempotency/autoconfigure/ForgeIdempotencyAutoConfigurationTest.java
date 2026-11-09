package io.github.foundry4j.forge.idempotency.autoconfigure;

import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import io.github.foundry4j.forge.idempotency.IdempotentStore;
import io.github.foundry4j.forge.idempotency.InMemoryIdempotentStore;
import io.github.foundry4j.forge.idempotency.RedisIdempotentStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/// Verifies that the provider property switches between the local and the shared store.
class ForgeIdempotencyAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ForgeRedisIdempotencyAutoConfiguration.class,
                    ForgeIdempotencyAutoConfiguration.class))
            .withBean(ExpressionEvaluator.class, ExpressionEvaluator::new);

    @Test
    void defaultsToTheInMemoryStore() {
        runner.run(context -> assertThat(context).getBean(IdempotentStore.class)
                .isInstanceOf(InMemoryIdempotentStore.class));
    }

    @Test
    void usesRedisWhenRequested() {
        runner.withPropertyValues("forge.idempotency.provider=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> assertThat(context).getBean(IdempotentStore.class)
                        .isInstanceOf(RedisIdempotentStore.class));
    }
}
