package io.github.foundry4j.forge.audit.entity;

import io.github.foundry4j.forge.data.audit.NotAudited;
import io.github.foundry4j.forge.data.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/// Persisted field level audit record.
///
/// One row is written per insert, update or delete. For updates [changes] contains
/// the JSON representation of the changed fields with their old and new values; for
/// inserts and deletes it contains the field snapshot of the entity.
@Entity
@Table(name = "forge_audit_log", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_actor", columnList = "actor")
})
@NotAudited
public class AuditLog extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 128)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "action", nullable = false, length = 16)
    private String action;

    @Column(name = "actor", length = 64)
    private String actor;

    @Column(name = "changes", length = 4000)
    private String changes;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /// Creates an empty entity for JPA.
    protected AuditLog() {
    }

    /// Creates a complete audit record.
    public AuditLog(String entityType, String entityId, String action, String actor,
                    String changes, Instant occurredAt) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actor = actor;
        this.changes = changes;
        this.occurredAt = occurredAt;
    }

    /// Simple class name of the audited entity.
    public String getEntityType() {
        return entityType;
    }

    /// Identifier of the audited entity.
    public String getEntityId() {
        return entityId;
    }

    /// `CREATE`, `UPDATE` or `DELETE`.
    public String getAction() {
        return action;
    }

    /// Actor who performed the change.
    public String getActor() {
        return actor;
    }

    /// JSON payload with the changed fields or the entity snapshot.
    public String getChanges() {
        return changes;
    }

    /// Time at which the change happened.
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
