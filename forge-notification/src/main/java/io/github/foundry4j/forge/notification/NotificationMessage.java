package io.github.foundry4j.forge.notification;

import io.github.foundry4j.forge.data.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/// Persisted notification.
///
/// The row is the notification log: it records what was sent, through which channel, to
/// whom, and whether delivery succeeded. Extending [TenantEntity] adds the tenant
/// discriminator, audit columns, optimistic locking and soft deletion.
@Entity
@Table(name = "forge_notification", indexes = {
        @Index(name = "idx_forge_notification_status", columnList = "status,next_attempt_at"),
        @Index(name = "idx_forge_notification_tenant", columnList = "tenant_id,created_at")
})
public class NotificationMessage extends TenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "recipient", nullable = false, length = 320)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "content", nullable = false, length = 65535)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    /// Required by JPA.
    protected NotificationMessage() {
    }

    /// Creates a pending notification that is eligible for immediate delivery.
    public NotificationMessage(NotificationChannel channel, String recipient, String subject, String content) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        this.subject = subject == null ? "" : subject;
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.status = NotificationStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = Instant.now();
    }

    /// Marks the notification as delivered.
    public void markSent(Instant now) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.lastError = null;
    }

    /// Records a failed delivery and schedules the next attempt.
    ///
    /// The delay grows exponentially up to a factor of sixty-four, which keeps a broken
    /// provider from being hammered while still recovering quickly from short outages. Once
    /// the attempt budget is exhausted the notification becomes [NotificationStatus#FAILED].
    public void markFailed(String error, Instant now, int maxAttempts, Duration retryDelay) {
        this.attempts++;
        this.lastError = truncate(error);
        if (this.attempts >= maxAttempts) {
            this.status = NotificationStatus.FAILED;
            return;
        }
        this.status = NotificationStatus.PENDING;
        int exponent = Math.min(this.attempts - 1, 6);
        this.nextAttemptAt = now.plus(retryDelay.multipliedBy(1L << exponent));
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }

    /// Delivery channel.
    public NotificationChannel getChannel() {
        return channel;
    }

    /// User identifier, address or phone number the notification is addressed to.
    public String getRecipient() {
        return recipient;
    }

    /// Short summary.
    public String getSubject() {
        return subject;
    }

    /// Notification body.
    public String getContent() {
        return content;
    }

    /// Current lifecycle state.
    public NotificationStatus getStatus() {
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

    /// Earliest instant at which the notification may be attempted again.
    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    /// Instant of the successful delivery, `null` while pending.
    public Instant getSentAt() {
        return sentAt;
    }
}
