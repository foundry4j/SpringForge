package io.github.foundry4j.forge.security.autoconfigure;

import io.github.foundry4j.forge.core.autoconfigure.ForgeCoreAutoConfiguration;
import io.github.foundry4j.forge.security.AuthContext;
import io.github.foundry4j.forge.security.JwtProperties;
import io.github.foundry4j.forge.security.JwtTokenService;
import io.github.foundry4j.forge.security.aspect.RbacAspect;
import io.github.foundry4j.forge.security.filter.JwtAuthenticationFilter;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/// Wires JWT authentication and role based access control.
@AutoConfiguration(after = ForgeCoreAutoConfiguration.class)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityAutoConfiguration {

    /// Principal query API, available even without JWT so that jobs can bind an identity.
    @Bean
    @ConditionalOnMissingBean
    public AuthContext forgeAuthContext() {
        return new AuthContext();
    }

    /// Token issuing and verification, enabled when a secret is configured.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "forge.jwt", name = "secret")
    public JwtTokenService forgeJwtTokenService(JwtProperties properties,
                                                ObjectProvider<ObjectMapper> objectMapper) {
        ObjectMapper mapper = objectMapper.getIfAvailable(() -> JsonMapper.builder().build());
        return new JwtTokenService(properties, mapper);
    }

    /// Authenticates bearer tokens before the request reaches the controller.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "forge.jwt", name = "secret")
    public FilterRegistrationBean<JwtAuthenticationFilter> forgeJwtAuthenticationFilter(
            JwtTokenService tokenService) {
        var registration = new FilterRegistrationBean<>(new JwtAuthenticationFilter(tokenService));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 30);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /// Enforces `@RequiresRole` and `@RequiresPermission`.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Aspect.class)
    public RbacAspect forgeRbacAspect(AuthContext authContext) {
        return new RbacAspect(authContext);
    }
}
