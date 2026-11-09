package io.github.foundry4j.forge.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the Quartz based scheduling layer.
///
/// @param enabled      whether scheduling beans are registered
/// @param group        default job and trigger group
/// @param autoRegister whether methods annotated with [ForgeScheduled] are registered
@ConfigurationProperties("forge.scheduling")
public record ForgeSchedulingProperties(
        Boolean enabled,
        String group,
        Boolean autoRegister
) {

    /// Applies defaults.
    public ForgeSchedulingProperties {
        enabled = enabled == null || enabled;
        group = group == null || group.isBlank() ? "forge" : group;
        autoRegister = autoRegister == null || autoRegister;
    }
}
