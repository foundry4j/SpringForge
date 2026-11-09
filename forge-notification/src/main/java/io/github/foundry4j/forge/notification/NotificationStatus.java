package io.github.foundry4j.forge.notification;

/// Lifecycle of a persisted notification.
public enum NotificationStatus {

    /// Waiting for delivery, either the first attempt or a retry.
    PENDING,

    /// Delivered successfully.
    SENT,

    /// The attempt budget was exhausted; the notification needs manual intervention.
    FAILED
}
