package io.github.foundry4j.forge.notification.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Registry of open notification sessions, at most one per user.
///
/// When a user opens a second tab the older session is closed, which keeps the
/// delivery path simple: the inbox and the WebSocket stream are the same data, so a
/// duplicate session would only duplicate messages.
public class NotificationSessionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /// Registers the session of a user and replaces a previous session.
    public void register(String userId, WebSocketSession session) {
        WebSocketSession previous = sessions.put(userId, session);
        if (previous != null && previous.isOpen()) {
            try {
                previous.close();
            } catch (IOException _) {
                // The stale session is replaced either way
            }
        }
    }

    /// Removes a session.
    public void unregister(WebSocketSession session) {
        sessions.values().removeIf(current -> current.getId().equals(session.getId()));
    }

    /// Sends a text message to a user.
    ///
    /// @return `true` when a session received the message
    public boolean send(String userId, String payload) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(payload));
            return true;
        } catch (IOException _) {
            sessions.remove(userId, session);
            return false;
        }
    }

    /// Whether a user has an open session.
    public boolean online(String userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /// Number of open sessions.
    public int onlineCount() {
        return (int) sessions.values().stream().filter(WebSocketSession::isOpen).count();
    }
}
