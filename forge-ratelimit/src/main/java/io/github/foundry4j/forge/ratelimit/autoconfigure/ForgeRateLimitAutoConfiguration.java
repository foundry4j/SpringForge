package io.github.foundry4j.forge.ratelimit.autoconfigure;

import io.github.foundry4j.forge.ratelimit.ForgeRateLimitProperties;
import io.github.foundry4j.forge.ratelimit.InMemoryRateLimiter;
import io.github.foundry4j.forge.ratelimit.RateLimitAspect;
import io.github.foundry4j.forge.ratelimit.RateLimitSubjectResolver;
import io.github.foundry4j.forge.ratelimit.RateLimiter;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Wires the rate limiting aspect.
///
/// The default limiter is the single node [InMemoryRateLimiter]. Setting
/// `forge.ratelimit.provider=redis` replaces it with the shared implementation from
/// [ForgeRedisRateLimitAutoConfiguration]. Any [RateLimiter] bean declared by the
/// application takes precedence over both.
@AutoConfiguration
@ConditionalOnClass(Aspect.class)
@EnableConfigurationProperties(ForgeRateLimitProperties.class)
public class ForgeRateLimitAutoConfiguration {

    private static final RateLimitSubjectResolver LOCAL = (strategy, method, target, args) -> "local";

    /// Default single node limiter.
    @Bean
    @ConditionalOnMissingBean
    public RateLimiter forgeRateLimiter() {
        return new InMemoryRateLimiter();
    }

    /// Rate limiting aspect.
    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect forgeRateLimitAspect(RateLimiter rateLimiter,
                                                ObjectProvider<RateLimitSubjectResolver> resolver) {
        return new RateLimitAspect(rateLimiter, resolver.getIfAvailable(() -> LOCAL));
    }
}
