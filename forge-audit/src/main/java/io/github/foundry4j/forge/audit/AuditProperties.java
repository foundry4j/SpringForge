package io.github.foundry4j.forge.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the audit module.
///
/// @param enabled whether entity changes are captured
@ConfigurationProperties("forge.audit")
public record AuditProperties(Boolean enabled) {

    /// Auditing is enabled by default.
    public AuditProperties {
        enabled = enabled == null || enabled;
    }
}
