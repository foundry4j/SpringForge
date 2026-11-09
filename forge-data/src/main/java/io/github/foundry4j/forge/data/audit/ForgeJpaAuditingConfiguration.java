package io.github.foundry4j.forge.data.audit;

import io.github.foundry4j.forge.core.context.ForgeContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/// Enables Spring Data JPA auditing and provides the current auditor.
///
/// The auditor is read from [ForgeContext], which the web filter fills from the
/// `X-User-Id` header. Background jobs fall back to `system`.
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "forgeAuditorAware")
public class ForgeJpaAuditingConfiguration {

    /// Name of the auditor provider bean referenced by `@EnableJpaAuditing`.
    public static final String AUDITOR_BEAN = "forgeAuditorAware";

    /// Provides the identifier written into `created_by` and `updated_by`.
    @Bean(name = AUDITOR_BEAN)
    public AuditorAware<String> forgeAuditorAware() {
        return () -> Optional.of(ForgeContext.userId().orElse("system"));
    }
}
