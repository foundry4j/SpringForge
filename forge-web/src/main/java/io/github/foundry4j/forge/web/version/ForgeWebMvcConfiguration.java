package io.github.foundry4j.forge.web.version;

import io.github.foundry4j.forge.web.ForgeWebProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/// Registers the API version interceptor for every MVC handler.
public class ForgeWebMvcConfiguration implements WebMvcConfigurer {

    private final ForgeWebProperties properties;

    /// Creates the configuration.
    public ForgeWebMvcConfiguration(ForgeWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiVersionInterceptor(properties)).addPathPatterns("/**");
    }
}
