package io.github.foundry4j.forge.notification;

/// Delivery channel of a notification.
///
/// Every channel is backed by a [NotificationSender] bean. The framework ships a logging
/// implementation for each value, so an application can adopt the module without an SMTP
/// server or an SMS gateway and replace the sender later.
public enum NotificationChannel {

    /// In-application notification. It is stored in [NotificationMessage] and shown to the
    /// user by the application itself.
    SYSTEM,

    /// Electronic mail.
    EMAIL,

    /// Short message service.
    SMS
}
