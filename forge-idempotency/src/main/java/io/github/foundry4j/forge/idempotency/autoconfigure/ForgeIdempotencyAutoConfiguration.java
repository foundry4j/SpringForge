package io.github.foundry4j.forge.idempotency.autoconfigure;

import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import io.github.foundry4j.forge.idempotency.ExpressionIdempotentKeyResolver;
import io.github.foundry4j.forge.idempotency.ForgeIdempotencyProperties;
import io.github.foundry4j.forge.idempotency.IdempotentAspect;
import io.github.foundry4j.forge.idempotency.IdempotentKeyResolver;
import io.github.foundry4j.forge.idempotency.IdempotentStore;
import io.github.foundry4j.forge.idempotency.InMemoryIdempotentStore;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/// Wires the idempotency aspect.
///
/// The default store is the single node [InMemoryIdempotentStore]. Setting
/// `forge.idempotency.provider=redis` replaces it with the shared implementation from
/// [ForgeRedisIdempotencyAutoConfiguration]. Any [IdempotentStore] bean declared by the
/// application takes precedence over both.
@AutoConfiguration
@ConditionalOnClass(Aspect.class)
@EnableConfigurationProperties(ForgeIdempotencyProperties.class)
public class ForgeIdempotencyAutoConfiguration {

    /// Default single node store.
    @Bean
    @ConditionalOnMissingBean
    public IdempotentStore forgeIdempotentStore() {
        return new InMemoryIdempotentStore();
    }

    /// Idempotency aspect.
    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect forgeIdempotentAspect(IdempotentStore store,
                                                  ExpressionEvaluator evaluator,
                                                  ObjectProvider<ObjectMapper> objectMapper,
                                                  ObjectProvider<IdempotentKeyResolver> resolver) {
        ObjectMapper mapper = objectMapper.getIfAvailable(() -> JsonMapper.builder().build());
        IdempotentKeyResolver keyResolver = resolver.getIfAvailable(
                () -> new ExpressionIdempotentKeyResolver(evaluator, mapper));
        return new IdempotentAspect(store, keyResolver, mapper);
    }
}
