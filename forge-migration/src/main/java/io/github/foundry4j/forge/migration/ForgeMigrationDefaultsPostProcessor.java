package io.github.foundry4j.forge.migration;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/// Applies Flyway defaults without overriding explicit application settings.
///
/// Adding `forge-migration` therefore turns on startup migrations against
/// `classpath:db/migration`, baselines an existing schema and leaves every value
/// overridable through `application.properties` or the environment.
public class ForgeMigrationDefaultsPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE_NAME = "forgeMigrationDefaults";

    private static final Map<String, String> DEFAULTS = Map.of(
            "spring.flyway.enabled", "true",
            "spring.flyway.baseline-on-migrate", "true",
            "spring.flyway.locations", "classpath:db/migration");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        DEFAULTS.forEach((key, value) -> {
            if (!environment.containsProperty(key)) {
                defaults.put(key, value);
            }
        });
        if (!defaults.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
