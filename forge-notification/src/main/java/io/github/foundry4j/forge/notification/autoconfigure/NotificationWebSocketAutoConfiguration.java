package io.github.foundry4j.forge.notification.autoconfigure;

import io.github.foundry4j.forge.notification.NotificationProperties;
import io.github.foundry4j.forge.notification.NotificationRealtimePublisher;
import io.github.foundry4j.forge.notification.NotificationService;
import io.github.foundry4j.forge.notification.websocket.NotificationSessionManager;
import io.github.foundry4j.forge.notification.websocket.NotificationWebSocketConfiguration;
import io.github.foundry4j.forge.notification.websocket.NotificationWebSocketHandler;
import io.github.foundry4j.forge.notification.websocket.WebSocketNotificationPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.WebSocketHandler;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/// Activates the real-time notification socket when Spring WebSocket is present.
@AutoConfiguration(after = ForgeNotificationAutoConfiguration.class)
@ConditionalOnClass(WebSocketHandler.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class NotificationWebSocketAutoConfiguration {

    /// Open sessions of notification clients.
    @Bean
    @ConditionalOnMissingBean
    public NotificationSessionManager forgeNotificationSessionManager() {
        return new NotificationSessionManager();
    }

    /// Pushes notifications to open sessions.
    @Bean
    @ConditionalOnMissingBean
    public NotificationRealtimePublisher forgeNotificationRealtimePublisher(
            NotificationSessionManager sessions, ObjectProvider<ObjectMapper> objectMapper) {
        ObjectMapper mapper = objectMapper.getIfAvailable(() -> JsonMapper.builder().build());
        return new WebSocketNotificationPublisher(sessions, mapper);
    }

    /// Socket endpoint, including the offline compensation on connect.
    @Bean
    @ConditionalOnMissingBean
    public NotificationWebSocketHandler forgeNotificationWebSocketHandler(
            NotificationSessionManager sessions, NotificationService service) {
        return new NotificationWebSocketHandler(sessions, service);
    }

    /// Registers the endpoint at the configured path.
    @Bean
    @ConditionalOnMissingBean
    public NotificationWebSocketConfiguration forgeNotificationWebSocketConfiguration(
            NotificationWebSocketHandler handler, NotificationProperties properties) {
        return new NotificationWebSocketConfiguration(handler, properties);
    }
}
