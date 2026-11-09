package io.github.foundry4j.forge.notification;

import io.github.foundry4j.forge.core.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests delivery, retry and sender resolution of the notification service.
class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationProperties properties = new NotificationProperties(
            true, true, 10, 2, Duration.ofSeconds(5), Duration.ofSeconds(1));

    @Test
    void deliversThroughTheMatchingPlugin() {
        List<NotificationMessage> delivered = new ArrayList<>();
        NotificationSender email = sender(NotificationChannel.EMAIL, delivered::add);
        var service = new NotificationService(repository, List.of(email), properties);

        NotificationMessage message = service.send(
                NotificationRequest.email("ada@example.com", "Order paid", "Thanks"));

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(message.getSentAt()).isNotNull();
        assertThat(delivered).singleElement().isSameAs(message);
    }

    @Test
    void keepsFailedNotificationsPendingForRetry() {
        NotificationSender email = sender(NotificationChannel.EMAIL, _ -> {
            throw new IllegalStateException("smtp down");
        });
        var service = new NotificationService(repository, List.of(email), properties);

        NotificationMessage message = service.send(
                NotificationRequest.email("ada@example.com", "Order paid", "Thanks"));

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getLastError()).isEqualTo("smtp down");
        assertThat(message.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void marksNotificationsFailedWhenTheBudgetIsExhausted() {
        NotificationSender email = sender(NotificationChannel.EMAIL, _ -> {
            throw new IllegalStateException("smtp down");
        });
        var exhausted = new NotificationProperties(true, false, 10, 1,
                Duration.ofSeconds(5), Duration.ofSeconds(1));
        var service = new NotificationService(repository, List.of(email), exhausted);

        NotificationMessage message = service.send(
                NotificationRequest.email("ada@example.com", "Order paid", "Thanks"));

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void fallsBackToBuiltInSendersForUncoveredChannels() {
        var service = new NotificationService(repository, List.of(), properties);

        assertThat(service.send(NotificationRequest.system("user-1", "Hi", "Body")).getStatus())
                .isEqualTo(NotificationStatus.SENT);
        assertThat(service.send(NotificationRequest.email("ada@example.com", "Hi", "Body")).getStatus())
                .isEqualTo(NotificationStatus.SENT);
        assertThat(service.send(NotificationRequest.sms("13800138000", "Hi", "Body")).getStatus())
                .isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void applicationPluginsReplaceTheBuiltInSenderOfTheSameChannel() {
        NotificationSender email = sender(NotificationChannel.EMAIL, _ -> {
            throw new IllegalStateException("custom provider");
        });
        var service = new NotificationService(repository, List.of(email), properties);

        NotificationMessage message = service.send(
                NotificationRequest.email("ada@example.com", "Order paid", "Thanks"));

        assertThat(message.getLastError()).isEqualTo("custom provider");
    }

    @Test
    void dispatchesDueNotifications() {
        var message = new NotificationMessage(NotificationChannel.SMS, "13800138000", "Hi", "Body");
        when(repository.findByStatusAndNextAttemptAtBeforeOrderByIdAsc(
                eq(NotificationStatus.PENDING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(message));
        var service = new NotificationService(repository, List.of(), properties);

        assertThat(service.dispatchPending()).isEqualTo(1);
        assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void retriesASingleNotification() {
        var message = new NotificationMessage(NotificationChannel.EMAIL, "ada@example.com", "Hi", "Body");
        when(repository.findById(7L)).thenReturn(Optional.of(message));
        var service = new NotificationService(repository, List.of(), properties);

        assertThat(service.retry(7L).getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void reportsMissingNotificationsOnRetry() {
        when(repository.findById(8L)).thenReturn(Optional.empty());
        var service = new NotificationService(repository, List.of(), properties);

        assertThatThrownBy(() -> service.retry(8L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void persistsTheNotificationBeforeDelivery() {
        var service = new NotificationService(repository, List.of(), properties);

        service.send(NotificationRequest.email("ada@example.com", "Hi", "Body"));

        var captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(captor.getValue().getRecipient()).isEqualTo("ada@example.com");
    }

    @Test
    void countsPendingNotifications() {
        when(repository.countByStatus(NotificationStatus.PENDING)).thenReturn(4L);
        var service = new NotificationService(repository, List.of(), properties);

        assertThat(service.pendingCount()).isEqualTo(4);
    }

    private static NotificationSender sender(NotificationChannel channel, Consumer<NotificationMessage> action) {
        return new NotificationSender() {
            @Override
            public NotificationChannel channel() {
                return channel;
            }

            @Override
            public void send(NotificationMessage message) {
                action.accept(message);
            }
        };
    }
}
