package io.github.foundry4j.forge.audit;

import io.github.foundry4j.forge.data.audit.AuditSupport;
import io.github.foundry4j.forge.data.audit.FieldChange;
import io.github.foundry4j.forge.data.entity.BaseEntity;
import org.hibernate.Hibernate;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/// Writes [AuditSupport] events into `forge_audit_log`.
///
/// The rows are inserted with a plain JDBC statement instead of a JPA repository:
/// the listener runs inside the flush of the audited entity, and enqueueing another
/// entity there would be unsafe because Hibernate is already executing its action
/// queue. The insert still uses the transaction connection, so an audit row and the
/// business change commit or roll back together.
public class AuditRecorder implements AuditSupport {

    private static final String INSERT = """
            insert into forge_audit_log
                (created_at, updated_at, created_by, updated_by, version, deleted,
                 entity_type, entity_id, action, actor, changes, occurred_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AuditActorProvider actorProvider;
    private final ObjectMapper objectMapper;
    private final AuditProperties properties;

    /// Creates the recorder.
    public AuditRecorder(JdbcTemplate jdbcTemplate, AuditActorProvider actorProvider,
                         ObjectMapper objectMapper, AuditProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.actorProvider = actorProvider;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void onCreate(Object entity, Map<String, String> state) {
        record(entity, "CREATE", json(state));
    }

    @Override
    public void onUpdate(Object entity, List<FieldChange> changes) {
        record(entity, "UPDATE", json(changes));
    }

    @Override
    public void onDelete(Object entity, Map<String, String> state) {
        record(entity, "DELETE", json(state));
    }

    private void record(Object entity, String action, String changes) {
        if (!properties.enabled()) {
            return;
        }
        Instant now = Instant.now();
        String actor = actorProvider.currentActor();
        jdbcTemplate.update(INSERT,
                Timestamp.from(now), Timestamp.from(now), actor, actor, 0L, false,
                entityType(entity), entityId(entity), action, actor, changes, Timestamp.from(now));
    }

    private String entityType(Object entity) {
        return Hibernate.unproxy(entity).getClass().getSimpleName();
    }

    private String entityId(Object entity) {
        return entity instanceof BaseEntity base && base.getId() != null
                ? String.valueOf(base.getId())
                : null;
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
