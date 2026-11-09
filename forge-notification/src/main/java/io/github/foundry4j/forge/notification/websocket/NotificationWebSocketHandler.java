package io.github.foundry4j.forge.notification.websocket;

import io.github.foundry4j.forge.notification.NotificationService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/// WebSocket endpoint of the notification inbox.
///
/// On connect the session is registered and every notification that was stored while
/// the user was offline is pushed, which is the compensation for an interrupted
/// real-time stream. The server answers `ping` with `pong`, so a client can keep the
/// connection alive through proxies.
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final NotificationSessionManager sessions;
    private final NotificationService notificationService;

    /// Creates the handler.
    public NotificationWebSocketHandler(NotificationSessionManager sessions,
                                        NotificationService notificationService) {
        this.sessions = sessions;
        this.notificationService = notificationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = userId(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        sessions.register(userId, session);
        notificationService.pushUndelivered(userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.unregister(session);
    }

    private String userId(WebSocketSession session) {
        Object value = session.getAttributes().get(NotificationHandshakeInterceptor.USER_ID);
        return value == null ? null : value.toString();
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (Exception _) {
            // Closing a broken session must not fail the connection callback
        }
    }
}
