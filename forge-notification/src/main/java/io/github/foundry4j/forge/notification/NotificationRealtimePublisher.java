package io.github.foundry4j.forge.notification;

/// Pushes notifications to sessions that are online right now.
///
/// The WebSocket module provides the default implementation. Without it the method
/// returns `false` and the notification stays in the inbox, which is the offline
/// compensation path anyway.
public interface NotificationRealtimePublisher {

    /// Pushes one notification to the sessions of a user.
    ///
    /// @return `true` when at least one session received the message
    boolean push(String userId, NotificationMessage notification);

    /// Number of currently connected sessions.
    int onlineCount();
}
