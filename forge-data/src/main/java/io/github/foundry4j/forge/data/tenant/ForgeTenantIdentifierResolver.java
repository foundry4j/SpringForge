package io.github.foundry4j.forge.data.tenant;

import io.github.foundry4j.forge.core.context.ForgeContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/// Resolves the tenant for Hibernate's discriminator based multi-tenancy.
///
/// When no request context is bound, for example inside a scheduler, the configured
/// default tenant is returned so that background work still has a deterministic scope.
public final class ForgeTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private final String defaultTenant;

    /// Creates the resolver.
    public ForgeTenantIdentifierResolver(String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        return ForgeContext.tenantId().orElse(defaultTenant);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
