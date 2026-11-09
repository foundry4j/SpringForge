package io.github.foundry4j.forge.notification.websocket;

import io.github.foundry4j.forge.notification.NotificationMessage;
import io.github.foundry4j.forge.notification.NotificationRealtimePublisher;
import io.github.foundry4j.forge.notification.NotificationView;
import tools.jackson.databind.ObjectMapper;

/// [NotificationRealtimePublisher] backed by [NotificationSessionManager].
public class WebSocketNotificationPublisher implements NotificationRealtimePublisher {

    private final NotificationSessionManager sessions;
    private final ObjectMapper objectMapper;

    /// Creates the publisher.
    public WebSocketNotificationPublisher(NotificationSessionManager sessions, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean push(String userId, NotificationMessage notification) {
        return sessions.send(userId, objectMapper.writeValueAsString(NotificationView.from(notification)));
    }

    @Override
    public int onlineCount() {
        return sessions.onlineCount();
    }
}
