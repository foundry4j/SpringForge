package io.github.foundry4j.forge.ratelimit.autoconfigure;

import io.github.foundry4j.forge.ratelimit.ForgeRateLimitProperties;
import io.github.foundry4j.forge.ratelimit.RateLimiter;
import io.github.foundry4j.forge.ratelimit.RedisRateLimiter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/// Selects the shared Redis limiter when `forge.ratelimit.provider=redis`.
///
/// The class is loaded only when Spring Data Redis is present, so applications that do not
/// use Redis keep the in-memory default without extra dependencies.
@AutoConfiguration(before = ForgeRateLimitAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "forge.ratelimit", name = "provider", havingValue = "redis")
@EnableConfigurationProperties(ForgeRateLimitProperties.class)
public class ForgeRedisRateLimitAutoConfiguration {

    /// Shared limiter backed by Redis counters.
    @Bean
    @ConditionalOnMissingBean
    public RateLimiter forgeRedisRateLimiter(StringRedisTemplate template,
                                             ForgeRateLimitProperties properties) {
        return new RedisRateLimiter(template, properties.keyPrefix());
    }
}
