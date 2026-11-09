package io.github.foundry4j.forge.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.TenantId;

/// Base class for tenant scoped entities.
///
/// Hibernate fills the column on insert and appends `tenant_id = ?` to every query,
/// so a repository can never accidentally return rows of another tenant. The value is
/// taken from [io.github.foundry4j.forge.core.context.ForgeContext].
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;

    /// Tenant owning the row.
    public String getTenantId() {
        return tenantId;
    }

    /// Sets the tenant; normally Hibernate does this automatically.
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
