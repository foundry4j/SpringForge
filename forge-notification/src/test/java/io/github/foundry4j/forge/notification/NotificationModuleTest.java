package io.github.foundry4j.forge.notification;

import io.github.foundry4j.forge.core.context.ForgeContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/// Boots the module against H2 and verifies persistence, tenant scoping and delivery.
@SpringBootTest(classes = ForgeNotificationTestApplication.class)
class NotificationModuleTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void persistsNotificationsForTheCurrentTenant() {
        ForgeContext.runAs(new ForgeContext.Caller("tenant-a", "user-1", "trace-1"), () -> {
            NotificationMessage message = notificationService.send(
                    NotificationRequest.system("user-1", "Welcome", "Hello Ada"));

            assertThat(message.getId()).isNotNull();
            assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(notificationRepository.findById(message.getId())).get()
                    .extracting(NotificationMessage::getTenantId, NotificationMessage::getChannel,
                            NotificationMessage::getStatus)
                    .containsExactly("tenant-a", NotificationChannel.SYSTEM, NotificationStatus.SENT);
        });
    }
}
