package io.github.foundry4j.forge.notification.websocket;

import io.github.foundry4j.forge.core.context.ForgeContext;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/// Binds the user identifier to a notification WebSocket session.
///
/// The identifier comes from the `userId` query parameter, the `X-User-Id` header or
/// the request context. Deployments with authentication should validate the token in
/// the handshake and ignore the query parameter; it exists so that local development
/// and the sample application need no login.
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {

    /// Session attribute holding the user identifier.
    public static final String USER_ID = NotificationHandshakeInterceptor.class.getName() + ".userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String userId = queryParameter(request, "userId");
        if (userId == null) {
            userId = request.getHeaders().getFirst("X-User-Id");
        }
        if (userId == null) {
            userId = ForgeContext.userId().orElse(null);
        }
        if (userId == null || userId.isBlank()) {
            return false;
        }
        attributes.put(USER_ID, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do
    }

    private String queryParameter(ServerHttpRequest request, String name) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        String value = servletRequest.getServletRequest().getParameter(name);
        return value == null || value.isBlank() ? null : value;
    }
}
