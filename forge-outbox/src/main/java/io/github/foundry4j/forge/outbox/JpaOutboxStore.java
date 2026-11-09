package io.github.foundry4j.forge.outbox;

import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/// JPA implementation of [OutboxStore].
///
/// Claiming uses `SELECT ... FOR UPDATE` so that several application instances can run
/// the relay concurrently without delivering the same event twice.
public class JpaOutboxStore implements OutboxStore {

    private final OutboxRepository repository;
    private final OutboxProperties properties;

    /// Creates the store.
    public JpaOutboxStore(OutboxRepository repository, OutboxProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /// {@inheritDoc}
    @Override
    @Transactional
    public void enqueue(String messageId, String type, String destination, String payload,
                        String headers, String tenantId, Instant occurredAt) {
        repository.save(new OutboxEvent(messageId, type, destination, payload, headers, tenantId, occurredAt));
    }

    /// {@inheritDoc}
    @Override
    @Transactional
    public List<OutboxEvent> claim(int batchSize) {
        Instant now = Instant.now();
        List<OutboxEvent> events = repository.lockPending(
                OutboxStatus.PENDING, now, PageRequest.of(0, Math.max(1, batchSize)));
        events.forEach(event -> event.markProcessing(now));
        return events;
    }

    /// {@inheritDoc}
    @Override
    @Transactional
    public void markSent(long id, Instant sentAt) {
        repository.findById(id).ifPresent(event -> event.markSent(sentAt));
    }

    /// {@inheritDoc}
    @Override
    @Transactional
    public void markFailed(long id, String error, Instant now) {
        repository.findById(id).ifPresent(event ->
                event.markFailed(error, now, properties.maxAttempts(), properties.retryDelay()));
    }

    /// {@inheritDoc}
    @Override
    @Transactional
    public int recoverStuck(Duration timeout) {
        Instant now = Instant.now();
        return repository.recoverStuck(
                OutboxStatus.PENDING, OutboxStatus.PROCESSING, now.minus(timeout), now);
    }

    /// {@inheritDoc}
    @Override
    @Transactional
    public int purge(Duration retention) {
        return repository.purgeSent(OutboxStatus.SENT, Instant.now().minus(retention));
    }

    /// {@inheritDoc}
    @Override
    @Transactional(readOnly = true)
    public long pendingCount() {
        return repository.countByStatus(OutboxStatus.PENDING);
    }
}
