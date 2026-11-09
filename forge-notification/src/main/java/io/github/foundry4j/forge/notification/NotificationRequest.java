package io.github.foundry4j.forge.notification;

import java.util.Objects;

/// Value object describing the notification a caller wants to send.
///
/// Prefer the static factories, they document the channel at the call site:
///
/// ```java
/// notificationService.send(NotificationRequest.email("ada@example.com", "Order paid", "..."));
/// ```
///
/// @param channel   delivery channel
/// @param recipient user identifier for [NotificationChannel#SYSTEM], address or phone number otherwise
/// @param subject   short summary, may be blank
/// @param content   notification body
public record NotificationRequest(NotificationChannel channel, String recipient, String subject, String content) {

    /// Validates the mandatory attributes.
    public NotificationRequest {
        Objects.requireNonNull(channel, "channel must not be null");
        recipient = requireText(recipient, "recipient");
        subject = subject == null ? "" : subject;
        content = requireText(content, "content");
    }

    /// Creates an in-application notification.
    public static NotificationRequest system(String recipient, String subject, String content) {
        return new NotificationRequest(NotificationChannel.SYSTEM, recipient, subject, content);
    }

    /// Creates an e-mail notification.
    public static NotificationRequest email(String recipient, String subject, String content) {
        return new NotificationRequest(NotificationChannel.EMAIL, recipient, subject, content);
    }

    /// Creates an SMS notification.
    public static NotificationRequest sms(String recipient, String subject, String content) {
        return new NotificationRequest(NotificationChannel.SMS, recipient, subject, content);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
