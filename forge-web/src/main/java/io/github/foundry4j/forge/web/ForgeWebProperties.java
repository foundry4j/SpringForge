package io.github.foundry4j.forge.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the web layer.
///
/// @param responseWrapping  when `true`, successful controller responses are wrapped into
///                         [io.github.foundry4j.forge.core.result.ApiResult]
/// @param problemTypePrefix prefix used to build the `type` URI of problem details
@ConfigurationProperties("forge.web")
public record ForgeWebProperties(
        Boolean responseWrapping,
        String problemTypePrefix
) {

    /// Applies defaults that make the framework productive out of the box.
    public ForgeWebProperties {
        responseWrapping = responseWrapping == null || responseWrapping;
        problemTypePrefix = problemTypePrefix == null || problemTypePrefix.isBlank()
                ? "urn:forge:error:"
                : problemTypePrefix;
    }
}
