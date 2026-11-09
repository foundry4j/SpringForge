package io.github.foundry4j.forge.audit.repository;

import io.github.foundry4j.forge.audit.entity.AuditLog;
import io.github.foundry4j.forge.data.repository.BaseRepository;

import java.util.List;

/// Repository of [AuditLog] records.
public interface AuditLogRepository extends BaseRepository<AuditLog> {

    /// Field level history of one entity, newest first.
    List<AuditLog> findByEntityTypeAndEntityIdOrderByIdDesc(String entityType, String entityId);

    /// Changes performed by an actor, newest first.
    List<AuditLog> findTop100ByActorOrderByIdDesc(String actor);
}
