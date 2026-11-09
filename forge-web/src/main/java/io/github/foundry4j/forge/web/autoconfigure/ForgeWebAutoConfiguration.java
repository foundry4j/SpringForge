package io.github.foundry4j.forge.web.autoconfigure;

import io.github.foundry4j.forge.cache.store.KeyValueStore;
import io.github.foundry4j.forge.core.context.ForgeContextProperties;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import io.github.foundry4j.forge.web.ForgeWebProperties;
import io.github.foundry4j.forge.web.error.GlobalExceptionHandler;
import io.github.foundry4j.forge.web.filter.ForgeRequestContextFilter;
import io.github.foundry4j.forge.web.idempotent.IdempotencyAspect;
import io.github.foundry4j.forge.web.ratelimit.RateLimitAspect;
import io.github.foundry4j.forge.web.result.ApiResponseAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/// Wires validation, error handling, idempotency and rate limiting.
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ForgeWebProperties.class)
public class ForgeWebAutoConfiguration {

    /// RFC 9457 error rendering.
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler forgeGlobalExceptionHandler(ForgeWebProperties properties) {
        return new GlobalExceptionHandler(properties);
    }

    /// Wraps successful responses into the uniform envelope.
    @Bean
    @ConditionalOnMissingBean
    public ApiResponseAdvice forgeApiResponseAdvice(ForgeWebProperties properties) {
        return new ApiResponseAdvice(properties);
    }

    /// Binds tenant, user and trace identifiers for every request.
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<ForgeRequestContextFilter> forgeRequestContextFilter(
            ForgeContextProperties properties) {
        var registration = new FilterRegistrationBean<>(new ForgeRequestContextFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /// Idempotency aspect.
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyAspect forgeIdempotencyAspect(KeyValueStore store, ExpressionEvaluator evaluator) {
        return new IdempotencyAspect(store, evaluator);
    }

    /// Rate limiting aspect.
    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect forgeRateLimitAspect(KeyValueStore store, ExpressionEvaluator evaluator) {
        return new RateLimitAspect(store, evaluator);
    }
}
