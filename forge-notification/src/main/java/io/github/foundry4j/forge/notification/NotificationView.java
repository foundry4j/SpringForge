package io.github.foundry4j.forge.notification;

import java.time.Instant;

/// Serialized view of an inbox notification.
///
/// The entity itself is never rendered, because it still carries tenant, audit and
/// retry bookkeeping fields that clients must not see.
///
/// @param id          notification identifier
/// @param type        business type
/// @param title       short title
/// @param content     message body
/// @param link        link to the business object, may be `null`
/// @param actorId     user who triggered the event, may be `null`
/// @param read        whether the recipient has read it
/// @param readAt      instant of the read action, may be `null`
/// @param deliveredAt instant of the real-time push, may be `null`
/// @param createdAt   creation timestamp
public record NotificationView(Long id,
                               NotificationType type,
                               String title,
                               String content,
                               String link,
                               String actorId,
                               boolean read,
                               Instant readAt,
                               Instant deliveredAt,
                               Instant createdAt) {

    /// Creates a view from a persisted notification.
    public static NotificationView from(NotificationMessage message) {
        return new NotificationView(message.getId(), message.getType(), message.getSubject(),
                message.getContent(), message.getLink(), message.getActorId(), message.isRead(),
                message.getReadAt(), message.getDeliveredAt(), message.getCreatedAt());
    }
}
