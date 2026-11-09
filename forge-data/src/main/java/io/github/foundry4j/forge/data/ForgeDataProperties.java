package io.github.foundry4j.forge.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the data layer.
///
/// @param workerId Snowflake worker identifier, unique per application instance
@ConfigurationProperties("forge.data")
public record ForgeDataProperties(
        Long workerId
) {

    /// Defaults the worker identifier to zero, which fits single instance deployments.
    public ForgeDataProperties {
        workerId = workerId == null ? 0L : workerId;
    }
}
