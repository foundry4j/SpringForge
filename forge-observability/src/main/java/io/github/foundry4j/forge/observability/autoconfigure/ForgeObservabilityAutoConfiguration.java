package io.github.foundry4j.forge.observability.autoconfigure;

import io.github.foundry4j.forge.observability.BearerAuthOpenApiCustomizer;
import io.github.foundry4j.forge.observability.ForgeHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Wires the framework health probe and the optional OpenAPI security scheme.
@AutoConfiguration
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnProperty(prefix = "forge.observability", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class ForgeObservabilityAutoConfiguration {

    /// Reports the framework itself as healthy.
    @Bean
    @ConditionalOnMissingBean(name = "forgeHealthIndicator")
    public ForgeHealthIndicator forgeHealthIndicator() {
        return new ForgeHealthIndicator();
    }

    /// Adds the bearer scheme when springdoc is on the classpath.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springdoc.core.customizers.OpenApiCustomizer")
    static class OpenApiConfiguration {

        /// Registers the bearer security scheme customizer.
        @Bean
        @ConditionalOnMissingBean
        BearerAuthOpenApiCustomizer forgeBearerAuthOpenApiCustomizer() {
            return new BearerAuthOpenApiCustomizer();
        }
    }
}
