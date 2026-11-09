package io.github.foundry4j.forge.data.audit;

import java.util.List;
import java.util.Map;

/// Service provider interface for audit events emitted by [AuditEntityListener].
///
/// The data module only defines the contract. An implementation is supplied by the
/// audit module; without one the listener is a no-op, so applications that do not
/// need auditing pay almost nothing.
public interface AuditSupport {

    /// Called after an entity was inserted.
    ///
    /// @param entity inserted entity
    /// @param state  snapshot of its persistent fields
    void onCreate(Object entity, Map<String, String> state);

    /// Called before an entity update is flushed.
    ///
    /// @param entity  updated entity
    /// @param changes field level differences, never empty
    void onUpdate(Object entity, List<FieldChange> changes);

    /// Called before an entity is removed.
    ///
    /// @param entity removed entity
    /// @param state  snapshot of its persistent fields
    void onDelete(Object entity, Map<String, String> state);
}
