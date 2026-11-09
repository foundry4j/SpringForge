package io.github.foundry4j.forge.observability;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/// Liveness style health indicator reporting that the framework itself is up.
///
/// The check never touches a dependency, so it answers "is the application process
/// healthy" instead of duplicating the database or broker probes that Boot already ships.
public class ForgeHealthIndicator implements HealthIndicator {

    /// Name of the health component.
    public static final String COMPONENT = "forge";

    @Override
    public Health health() {
        return Health.up().withDetail("framework", "forge").build();
    }
}
