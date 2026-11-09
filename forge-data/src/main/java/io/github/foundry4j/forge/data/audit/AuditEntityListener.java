package io.github.foundry4j.forge.data.audit;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/// JPA entity listener that turns entity lifecycle events into audit events.
///
/// Attached to [io.github.foundry4j.forge.data.entity.BaseEntity], so every entity
/// of the framework is audited without extra annotations:
///
/// - insert: a field snapshot is handed to [AuditSupport#onCreate]
/// - update: the snapshot taken on load is diffed against the current state and the
///   changes are handed to [AuditSupport#onUpdate]
/// - delete: the last snapshot is handed to [AuditSupport#onDelete]
///
/// Entities annotated with [NotAudited] are skipped. Bulk JPQL and native updates do
/// not trigger lifecycle callbacks and are therefore not audited.
public class AuditEntityListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuditEntityListener.class);

    /// Snapshot per loaded entity. Weak keys make sure the map never keeps entities alive.
    private static final Map<Object, Map<String, String>> SNAPSHOTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /// Framework maintained fields are noise in an audit trail.
    private static final Set<String> IGNORED_FIELDS =
            Set.of("id", "createdAt", "updatedAt", "createdBy", "updatedBy", "version", "tenantId");

    @PostLoad
    public void postLoad(Object entity) {
        if (auditable(entity)) {
            SNAPSHOTS.put(entity, snapshot(entity));
        }
    }

    @PostPersist
    public void postPersist(Object entity) {
        if (!auditable(entity)) {
            return;
        }
        Map<String, String> state = snapshot(entity);
        SNAPSHOTS.put(entity, state);
        AuditSupportHolder.support().ifPresent(support -> invoke(
                () -> support.onCreate(entity, state), entity, "create"));
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (!auditable(entity)) {
            return;
        }
        Map<String, String> before = SNAPSHOTS.get(entity);
        if (before == null) {
            before = snapshot(entity);
        }
        List<FieldChange> changes = diff(before, snapshot(entity));
        if (changes.isEmpty()) {
            return;
        }
        List<FieldChange> captured = List.copyOf(changes);
        AuditSupportHolder.support().ifPresent(support -> invoke(
                () -> support.onUpdate(entity, captured), entity, "update"));
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        if (auditable(entity)) {
            SNAPSHOTS.put(entity, snapshot(entity));
        }
    }

    @PreRemove
    public void preRemove(Object entity) {
        if (!auditable(entity)) {
            return;
        }
        Map<String, String> state = SNAPSHOTS.getOrDefault(entity, snapshot(entity));
        AuditSupportHolder.support().ifPresent(support -> invoke(
                () -> support.onDelete(entity, state), entity, "delete"));
    }

    @PostRemove
    public void postRemove(Object entity) {
        SNAPSHOTS.remove(entity);
    }

    private static void invoke(Runnable action, Object entity, String event) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            LOG.warn("Audit {} event for {} failed", event, entity.getClass().getSimpleName(), ex);
        }
    }

    private static boolean auditable(Object entity) {
        return entity != null && !entity.getClass().isAnnotationPresent(NotAudited.class);
    }

    private static Map<String, String> snapshot(Object entity) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Class<?> type = entity.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!inspectable(field)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    values.put(field.getName(), stringify(field.get(entity)));
                } catch (ReflectiveOperationException | RuntimeException _) {
                    // A field that cannot be read is skipped instead of failing the audit
                }
            }
        }
        return values;
    }

    private static boolean inspectable(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
            return false;
        }
        return !IGNORED_FIELDS.contains(field.getName());
    }

    private static String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static List<FieldChange> diff(Map<String, String> before, Map<String, String> after) {
        Set<String> fields = new LinkedHashSet<>(before.keySet());
        fields.addAll(after.keySet());
        List<FieldChange> changes = new ArrayList<>();
        for (String field : fields) {
            String oldValue = before.get(field);
            String newValue = after.get(field);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new FieldChange(field, oldValue, newValue));
            }
        }
        return changes;
    }
}
