package io.github.foundry4j.forge.audit.repository;

import io.github.foundry4j.forge.audit.entity.OperationLogEntry;
import io.github.foundry4j.forge.data.repository.BaseRepository;

import java.time.Instant;
import java.util.List;

/// Repository of [OperationLogEntry] records.
public interface OperationLogRepository extends BaseRepository<OperationLogEntry> {

    /// Operation logs of one actor, newest first.
    List<OperationLogEntry> findTop100ByActorOrderByIdDesc(String actor);

    /// Operation logs written since a point in time, newest first.
    List<OperationLogEntry> findTop100ByOccurredAtAfterOrderByIdDesc(Instant since);
}
