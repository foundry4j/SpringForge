package io.github.foundry4j.forge.notification.websocket;

import io.github.foundry4j.forge.notification.NotificationProperties;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/// Publishes the notification socket at `forge.notification.web-socket-path`.
public class NotificationWebSocketConfiguration implements WebSocketConfigurer {

    private final NotificationWebSocketHandler handler;
    private final NotificationProperties properties;

    /// Creates the configuration.
    public NotificationWebSocketConfiguration(NotificationWebSocketHandler handler,
                                              NotificationProperties properties) {
        this.handler = handler;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, properties.webSocketPath())
                .addInterceptors(new NotificationHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }
}
