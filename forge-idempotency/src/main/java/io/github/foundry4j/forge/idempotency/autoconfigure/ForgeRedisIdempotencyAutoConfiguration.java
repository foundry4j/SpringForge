package io.github.foundry4j.forge.idempotency.autoconfigure;

import io.github.foundry4j.forge.idempotency.ForgeIdempotencyProperties;
import io.github.foundry4j.forge.idempotency.IdempotentStore;
import io.github.foundry4j.forge.idempotency.RedisIdempotentStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/// Selects the shared Redis store when `forge.idempotency.provider=redis`.
///
/// The class is loaded only when Spring Data Redis is present, so applications that do not
/// use Redis keep the in-memory default without extra dependencies.
@AutoConfiguration(before = ForgeIdempotencyAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "forge.idempotency", name = "provider", havingValue = "redis")
@EnableConfigurationProperties(ForgeIdempotencyProperties.class)
public class ForgeRedisIdempotencyAutoConfiguration {

    /// Shared store backed by Redis keys.
    @Bean
    @ConditionalOnMissingBean
    public IdempotentStore forgeRedisIdempotentStore(StringRedisTemplate template,
                                                     ForgeIdempotencyProperties properties) {
        return new RedisIdempotentStore(template, properties.keyPrefix());
    }
}
