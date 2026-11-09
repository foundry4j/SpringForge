package io.github.foundry4j.forge.web.autoconfigure;

import io.github.foundry4j.forge.core.context.ForgeContextProperties;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import io.github.foundry4j.forge.idempotency.ExpressionIdempotentKeyResolver;
import io.github.foundry4j.forge.idempotency.IdempotentKeyResolver;
import io.github.foundry4j.forge.ratelimit.RateLimitSubjectResolver;
import io.github.foundry4j.forge.web.ForgeWebProperties;
import io.github.foundry4j.forge.web.error.GlobalExceptionHandler;
import io.github.foundry4j.forge.web.file.FileStorage;
import io.github.foundry4j.forge.web.file.LocalFileStorage;
import io.github.foundry4j.forge.web.filter.AccessLogFilter;
import io.github.foundry4j.forge.web.filter.ForgeRequestContextFilter;
import io.github.foundry4j.forge.web.idempotency.HttpIdempotentKeyResolver;
import io.github.foundry4j.forge.web.ratelimit.WebRateLimitSubjectResolver;
import io.github.foundry4j.forge.web.result.ApiResponseAdvice;
import io.github.foundry4j.forge.web.version.ForgeWebMvcConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/// Wires validation, error handling and the HTTP resolvers used by the guard modules.
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

    /// Writes one access log line per request.
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<AccessLogFilter> forgeAccessLogFilter(ForgeWebProperties properties) {
        var registration = new FilterRegistrationBean<>(new AccessLogFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /// Checks `@ApiVersion` endpoints and publishes the negotiated version.
    @Bean
    @ConditionalOnMissingBean
    public WebMvcConfigurer forgeApiVersionConfigurer(ForgeWebProperties properties) {
        return new ForgeWebMvcConfiguration(properties);
    }

    /// Local disk file storage, replaceable by an application bean.
    @Bean
    @ConditionalOnMissingBean
    public FileStorage forgeFileStorage(ForgeWebProperties properties) {
        return new LocalFileStorage(properties.file());
    }

    /// Supplies idempotency keys from the request header.
    @Bean
    @ConditionalOnMissingBean
    public IdempotentKeyResolver forgeIdempotentKeyResolver(ExpressionEvaluator evaluator,
                                                            ObjectProvider<ObjectMapper> objectMapper) {
        ObjectMapper mapper = objectMapper.getIfAvailable(() -> JsonMapper.builder().build());
        return new HttpIdempotentKeyResolver(new ExpressionIdempotentKeyResolver(evaluator, mapper));
    }

    /// Supplies rate limit subjects from the authenticated user or the client address.
    @Bean
    @ConditionalOnMissingBean
    public RateLimitSubjectResolver forgeRateLimitSubjectResolver() {
        return new WebRateLimitSubjectResolver();
    }
}
