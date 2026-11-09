package io.github.foundry4j.forge.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests the validation and the channel factories of the request object.
class NotificationRequestTest {

    @Test
    void createsRequestsForEveryChannel() {
        assertThat(NotificationRequest.system("user-1", "Hi", "Body").channel())
                .isEqualTo(NotificationChannel.SYSTEM);
        assertThat(NotificationRequest.email("ada@example.com", "Hi", "Body").channel())
                .isEqualTo(NotificationChannel.EMAIL);
        assertThat(NotificationRequest.sms("13800138000", "Hi", "Body").channel())
                .isEqualTo(NotificationChannel.SMS);
    }

    @Test
    void normalizesAMissingSubject() {
        var request = new NotificationRequest(NotificationChannel.EMAIL, "ada@example.com", null, "Body");

        assertThat(request.subject()).isEmpty();
    }

    @Test
    void rejectsBlankRecipientsAndContent() {
        assertThatThrownBy(() -> NotificationRequest.email("", "Hi", "Body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipient");
        assertThatThrownBy(() -> NotificationRequest.email("ada@example.com", "Hi", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content");
    }
}
