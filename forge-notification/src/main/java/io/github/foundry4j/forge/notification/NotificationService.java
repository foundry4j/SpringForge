package io.github.foundry4j.forge.notification;

import io.github.foundry4j.forge.core.exception.Errors;
import io.github.foundry4j.forge.notification.sender.EmailNotificationSender;
import io.github.foundry4j.forge.notification.sender.SmsNotificationSender;
import io.github.foundry4j.forge.notification.sender.SystemNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/// Application facing entry point of the notification module.
///
/// Every call persists the notification before delivery, so the history survives restarts
/// and failed deliveries can be retried. Delivery itself is delegated to the
/// [NotificationSender] of the channel; the built-in senders log the message.
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final Map<NotificationChannel, NotificationSender> senders;
    private final NotificationProperties properties;

    /// Creates the service, falling back to the built-in senders of the channels that are
    /// not covered by an application supplied [NotificationSender] bean.
    public NotificationService(NotificationRepository repository, List<NotificationSender> senders,
                               NotificationProperties properties) {
        this.repository = repository;
        this.properties = properties;
        this.senders = resolve(senders);
    }

    /// Persists the notification and attempts the first delivery.
    ///
    /// A delivery failure does not propagate to the caller: the notification stays pending
    /// and is retried in the background, so a broken mail server never rolls back a business
    /// transaction.
    ///
    /// @return the persisted notification with its delivery state
    @Transactional
    public NotificationMessage send(NotificationRequest request) {
        NotificationMessage message = new NotificationMessage(request.channel(), request.recipient(),
                request.subject(), request.content());
        repository.save(message);
        deliver(message);
        return message;
    }

    /// Retries a single notification immediately.
    @Transactional
    public NotificationMessage retry(long id) {
        NotificationMessage message = repository.findById(id)
                .orElseThrow(() -> Errors.notFound("Notification", id));
        deliver(message);
        return message;
    }

    /// Delivers pending notifications whose retry time has come.
    ///
    /// @return number of notifications attempted
    @Transactional
    public int dispatchPending() {
        List<NotificationMessage> due = repository.findByStatusAndNextAttemptAtBeforeOrderByIdAsc(
                NotificationStatus.PENDING, Instant.now(), PageRequest.of(0, properties.batchSize()));
        due.forEach(this::deliver);
        return due.size();
    }

    /// Number of notifications waiting for delivery.
    @Transactional(readOnly = true)
    public long pendingCount() {
        return repository.countByStatus(NotificationStatus.PENDING);
    }

    private void deliver(NotificationMessage message) {
        NotificationSender sender = senders.get(message.getChannel());
        try {
            sender.send(message);
            message.markSent(Instant.now());
        } catch (RuntimeException ex) {
            LOG.warn("Notification [{}] via {} failed", message.getId(), message.getChannel(), ex);
            message.markFailed(ex.getMessage(), Instant.now(), properties.maxAttempts(), properties.retryDelay());
        }
    }

    private static Map<NotificationChannel, NotificationSender> resolve(List<NotificationSender> custom) {
        Map<NotificationChannel, NotificationSender> resolved = new EnumMap<>(NotificationChannel.class);
        resolved.put(NotificationChannel.SYSTEM, new SystemNotificationSender());
        resolved.put(NotificationChannel.EMAIL, new EmailNotificationSender());
        resolved.put(NotificationChannel.SMS, new SmsNotificationSender());
        for (NotificationSender sender : custom) {
            resolved.put(sender.channel(), sender);
        }
        return Map.copyOf(resolved);
    }
}
