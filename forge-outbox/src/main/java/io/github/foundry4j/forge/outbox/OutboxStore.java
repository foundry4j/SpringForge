package io.github.foundry4j.forge.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/// Storage contract of the transactional outbox.
public interface OutboxStore {

    /// Appends a pending event inside the current business transaction.
    void enqueue(String messageId, String type, String destination, String payload,
                 String headers, String tenantId, Instant occurredAt);

    /// Claims up to `batchSize` due events, locking them for this relay instance.
    List<OutboxEvent> claim(int batchSize);

    /// Marks an event as delivered.
    void markSent(long id, Instant sentAt);

    /// Marks a delivery as failed, scheduling a retry when budget is left.
    void markFailed(long id, String error, Instant now);

    /// Returns abandoned `PROCESSING` events to the pending state.
    int recoverStuck(Duration timeout);

    /// Deletes `SENT` events older than the retention window.
    int purge(Duration retention);

    /// Number of events waiting for delivery.
    long pendingCount();
}
