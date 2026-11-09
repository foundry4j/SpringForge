package io.github.foundry4j.forge.core.context;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// External configuration of the request context headers.
///
/// @param tenantHeader  header carrying the tenant identifier
/// @param userHeader    header carrying the authenticated user identifier
/// @param traceHeader   header carrying (or receiving) the trace identifier
/// @param defaultTenant tenant used when the request does not provide one
@ConfigurationProperties("forge.context")
public record ForgeContextProperties(
        String tenantHeader,
        String userHeader,
        String traceHeader,
        String defaultTenant
) {

    /// Applies sensible defaults so that the framework works without configuration.
    public ForgeContextProperties {
        tenantHeader = defaultIfBlank(tenantHeader, "X-Tenant-Id");
        userHeader = defaultIfBlank(userHeader, "X-User-Id");
        traceHeader = defaultIfBlank(traceHeader, "X-Trace-Id");
        defaultTenant = defaultIfBlank(defaultTenant, "default");
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
