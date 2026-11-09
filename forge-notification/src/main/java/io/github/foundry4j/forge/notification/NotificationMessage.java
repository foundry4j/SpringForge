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
        @Index(name = "idx_forge_notification_tenant", columnList = "tenant_id,created_at"),
        @Index(name = "idx_forge_notification_inbox", columnList = "recipient,channel,read_at")
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
    @Column(name = "type", nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "link", length = 512)
    private String link;

    @Column(name = "actor_id", length = 64)
    private String actorId;

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

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /// Required by JPA.
    protected NotificationMessage() {
    }

    /// Creates a pending notification that is eligible for immediate delivery.
    public NotificationMessage(NotificationChannel channel, String recipient, String subject, String content) {
        this(channel, recipient, subject, content, NotificationType.SYSTEM, null, null);
    }

    /// Creates a pending notification with type and business reference.
    public NotificationMessage(NotificationChannel channel, String recipient, String subject, String content,
                               NotificationType type, String link, String actorId) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        this.subject = subject == null ? "" : subject;
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.type = type == null ? NotificationType.SYSTEM : type;
        this.link = link;
        this.actorId = actorId;
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

    /// Marks the notification as handled although the channel had nothing to do, for
    /// example because the recipient has no e-mail address configured.
    public void markSkipped(String reason) {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
        this.lastError = truncate(reason);
    }

    /// Marks the in-application notification as pushed to an open session.
    public void markDelivered(Instant now) {
        this.deliveredAt = now;
    }

    /// Marks the notification as read by its recipient.
    public void markRead(Instant now) {
        if (this.readAt == null) {
            this.readAt = now;
        }
    }

    /// Replaces the recipient before delivery, used when a user id was resolved to an
    /// e-mail address or a phone number.
    public void retarget(String target) {
        this.recipient = Objects.requireNonNull(target, "target must not be null");
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

    /// Business type of the notification.
    public NotificationType getType() {
        return type;
    }

    /// Business object or URL the notification links to.
    public String getLink() {
        return link;
    }

    /// User who triggered the notification, `null` for system events.
    public String getActorId() {
        return actorId;
    }

    /// Instant at which the recipient read the notification, `null` while unread.
    public Instant getReadAt() {
        return readAt;
    }

    /// Instant at which the notification was pushed to an online recipient.
    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    /// Whether the recipient has read the notification.
    public boolean isRead() {
        return readAt != null;
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
