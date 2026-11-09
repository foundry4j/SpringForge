package io.github.foundry4j.forge.core.autoconfigure;

import io.github.foundry4j.forge.core.context.ForgeContextProperties;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Registers the small set of beans that every forge module builds upon.
@AutoConfiguration
@EnableConfigurationProperties(ForgeContextProperties.class)
public class ForgeCoreAutoConfiguration {

    /// Shared SpEL evaluator used by the annotation driven modules.
    @Bean
    @ConditionalOnMissingBean
    public ExpressionEvaluator forgeExpressionEvaluator() {
        return new ExpressionEvaluator();
    }
}
