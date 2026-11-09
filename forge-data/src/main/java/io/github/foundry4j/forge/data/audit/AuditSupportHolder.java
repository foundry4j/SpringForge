package io.github.foundry4j.forge.data.audit;

import java.util.Optional;

/// Bridges the JPA entity listener, which is created by Hibernate, to the Spring
/// context. The data auto-configuration binds the optional [AuditSupport] bean here.
public final class AuditSupportHolder {

    private static volatile AuditSupport support;

    private AuditSupportHolder() {
    }

    /// Binds the audit implementation; `null` disables auditing.
    public static void bind(AuditSupport auditSupport) {
        support = auditSupport;
    }

    /// Returns the bound implementation, if any.
    static Optional<AuditSupport> support() {
        return Optional.ofNullable(support);
    }
}
