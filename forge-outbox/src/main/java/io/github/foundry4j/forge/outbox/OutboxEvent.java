package io.github.foundry4j.forge.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/// Persisted outbox event.
///
/// The entity deliberately does not extend `BaseEntity`: outbox rows are infrastructure
/// data, they are hard deleted once delivered and must not be filtered by the soft
/// delete predicate of the business entities.
@Entity
@Table(name = "forge_outbox_event", indexes = @Index(
        name = "idx_forge_outbox_status", columnList = "status,next_attempt_at"))
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 64)
    private String messageId;

    @Column(name = "event_type", nullable = false, length = 160)
    private String type;

    @Column(name = "destination", nullable = false, length = 200)
    private String destination;

    @Column(name = "payload", nullable = false, length = 65535)
    private String payload;

    @Column(name = "message_headers", nullable = false, length = 65535)
    private String headers;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /// Required by JPA.
    protected OutboxEvent() {
    }

    /// Creates a pending event that is immediately eligible for delivery.
    public OutboxEvent(String messageId, String type, String destination, String payload,
                       String headers, String tenantId, Instant occurredAt) {
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.destination = destination == null ? "" : destination;
        this.payload = payload == null ? "" : payload;
        this.headers = headers == null ? "{}" : headers;
        this.tenantId = tenantId;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.nextAttemptAt = this.occurredAt;
        this.createdAt = this.occurredAt;
        this.updatedAt = this.occurredAt;
    }

    /// Marks the event as being delivered.
    public void markProcessing(Instant now) {
        this.status = OutboxStatus.PROCESSING;
        this.updatedAt = now;
    }

    /// Marks the event as delivered.
    public void markSent(Instant now) {
        this.status = OutboxStatus.SENT;
        this.sentAt = now;
        this.updatedAt = now;
        this.lastError = null;
    }

    /// Records a failed delivery and schedules the next attempt.
    ///
    /// The delay grows exponentially up to a factor of sixty-four, which keeps a broken
    /// broker from being hammered while still recovering quickly from short outages.
    public void markFailed(String error, Instant now, int maxAttempts, Duration retryDelay) {
        this.attempts++;
        this.lastError = truncate(error);
        this.updatedAt = now;
        if (this.attempts >= maxAttempts) {
            this.status = OutboxStatus.FAILED;
            return;
        }
        this.status = OutboxStatus.PENDING;
        int exponent = Math.min(this.attempts - 1, 6);
        this.nextAttemptAt = now.plus(retryDelay.multipliedBy(1L << exponent));
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }

    /// Surrogate primary key.
    public Long getId() {
        return id;
    }

    /// Unique identifier of the message, used for broker side de-duplication.
    public String getMessageId() {
        return messageId;
    }

    /// Logical event type.
    public String getType() {
        return type;
    }

    /// Routing key or topic suffix.
    public String getDestination() {
        return destination;
    }

    /// JSON serialized payload.
    public String getPayload() {
        return payload;
    }

    /// JSON serialized transport headers.
    public String getHeaders() {
        return headers;
    }

    /// Tenant the event was produced for, may be `null`.
    public String getTenantId() {
        return tenantId;
    }

    /// Current lifecycle state.
    public OutboxStatus getStatus() {
        return status;
    }

    /// Number of delivery attempts performed so far.
    public int getAttempts() {
        return attempts;
    }

    /// Message of the last delivery failure.
    public String getLastError() {
        return lastError;
    }

    /// Earliest instant at which the event may be claimed again.
    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    /// Instant at which the business operation happened.
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /// Instant at which the event was delivered, `null` while pending.
    public Instant getSentAt() {
        return sentAt;
    }

    /// Creation timestamp.
    public Instant getCreatedAt() {
        return createdAt;
    }

    /// Last update timestamp.
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
