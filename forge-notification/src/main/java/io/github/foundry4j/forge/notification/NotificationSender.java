package io.github.foundry4j.forge.notification;

/// Service provider interface for notification delivery.
///
/// The framework ships a logging implementation for every [NotificationChannel]. An
/// application plugs a real provider by declaring its own `NotificationSender` bean; the bean
/// replaces the built-in sender of the same channel:
///
/// ```java
/// @Bean
/// NotificationSender smtpSender(MailSender mailSender) {
///     return new SmtpNotificationSender(mailSender);
/// }
/// ```
///
/// Implementations must throw a runtime exception when delivery fails. The module records
/// the failure, keeps the notification pending and retries it with an exponential backoff.
public interface NotificationSender {

    /// Channel handled by this sender.
    NotificationChannel channel();

    /// Delivers the notification. Returning normally is treated as success.
    void send(NotificationMessage message);
}
