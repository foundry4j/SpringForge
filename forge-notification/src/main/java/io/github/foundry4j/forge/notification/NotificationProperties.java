package io.github.foundry4j.forge.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Configuration of the notification module.
///
/// @param enabled       whether the notification log and the SPI are registered
/// @param retryEnabled  whether pending notifications are retried in the background
/// @param batchSize     maximum number of notifications retried per pass
/// @param maxAttempts   delivery attempts before a notification is marked as failed
/// @param retryDelay    base delay used by the exponential backoff
/// @param pollInterval  pause between two retry passes
/// @param emailEnabled  whether user notifications are also fanned out to e-mail
/// @param smsEnabled    whether user notifications are also fanned out to SMS
/// @param webSocketPath endpoint of the real-time notification socket
@ConfigurationProperties("forge.notification")
public record NotificationProperties(
        Boolean enabled,
        Boolean retryEnabled,
        Integer batchSize,
        Integer maxAttempts,
        Duration retryDelay,
        Duration pollInterval,
        Boolean emailEnabled,
        Boolean smsEnabled,
        String webSocketPath
) {

    /// Applies sensible defaults for every optional property.
    public NotificationProperties {
        enabled = enabled == null || enabled;
        retryEnabled = retryEnabled == null || retryEnabled;
        batchSize = batchSize == null || batchSize < 1 ? 50 : batchSize;
        maxAttempts = maxAttempts == null || maxAttempts < 1 ? 3 : maxAttempts;
        retryDelay = retryDelay == null ? Duration.ofMinutes(1) : retryDelay;
        pollInterval = pollInterval == null ? Duration.ofSeconds(30) : pollInterval;
        emailEnabled = emailEnabled != null && emailEnabled;
        smsEnabled = smsEnabled != null && smsEnabled;
        webSocketPath = webSocketPath == null || webSocketPath.isBlank()
                ? "/ws/notifications"
                : webSocketPath;
    }
}
