package io.github.foundry4j.forge.audit;

import io.github.foundry4j.forge.audit.entity.OperationLogEntry;
import io.github.foundry4j.forge.audit.repository.OperationLogRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/// Writes operation log rows in an isolated transaction.
///
/// A separate bean is required so that the `REQUIRES_NEW` propagation takes effect
/// through the Spring proxy.
public class OperationLogWriter {

    private final OperationLogRepository repository;

    /// Creates the writer.
    public OperationLogWriter(OperationLogRepository repository) {
        this.repository = repository;
    }

    /// Persists the entry, committing even when the caller later fails.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(OperationLogEntry entry) {
        repository.save(entry);
    }
}
