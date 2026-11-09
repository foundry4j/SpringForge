package io.github.foundry4j.forge.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/// Entry point of the sample application.
///
/// Everything else is contributed by the forge modules through auto-configuration. The
/// application only adds its own entity and repository packages to the JPA scan, because
/// the framework modules already declare scanning for their own packages.
@SpringBootApplication
@EntityScan(basePackageClasses = SpringForgeApplication.class)
@EnableJpaRepositories(basePackageClasses = SpringForgeApplication.class)
public class SpringForgeApplication {

    /// Starts the application.
    public static void main(String[] args) {
        SpringApplication.run(SpringForgeApplication.class, args);
    }
}
