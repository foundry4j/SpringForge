package io.github.foundry4j.forge.notification;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import io.github.foundry4j.forge.core.exception.Errors;
import io.github.foundry4j.forge.core.result.PageResult;
import io.github.foundry4j.forge.data.support.PageResults;
import io.github.foundry4j.forge.notification.sender.EmailNotificationSender;
import io.github.foundry4j.forge.notification.sender.SmsNotificationSender;
import io.github.foundry4j.forge.notification.sender.SystemNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// Application facing entry point of the notification module.
///
/// Every call persists the notification before delivery, so the history survives restarts
/// and failed deliveries can be retried. Delivery itself is delegated to the
/// [NotificationSender] of the channel; the built-in senders log the message.
///
/// In-application notifications are additionally pushed to open WebSocket sessions and
/// can be read through the inbox methods, which back the REST controller of the module.
public class NotificationService implements NotificationInbox {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final Map<NotificationChannel, NotificationSender> senders;
    private final NotificationProperties properties;
    private final NotificationContactResolver contacts;
    private final NotificationRealtimePublisher publisher;
    private final SmsSender smsSender;

    /// Creates the service without real-time support.
    public NotificationService(NotificationRepository repository, List<NotificationSender> senders,
                               NotificationProperties properties) {
        this(repository, senders, properties, new NotificationContactResolver() {
        }, null, null);
    }

    /// Creates the service, falling back to the built-in senders of the channels that are
    /// not covered by an application supplied [NotificationSender] bean.
    public NotificationService(NotificationRepository repository, List<NotificationSender> senders,
                               NotificationProperties properties, NotificationContactResolver contacts,
                               NotificationRealtimePublisher publisher, SmsSender smsSender) {
        this.repository = repository;
        this.properties = properties;
        this.contacts = contacts == null ? new NotificationContactResolver() {
        } : contacts;
        this.publisher = publisher;
        this.smsSender = smsSender;
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

    /// Stores an in-application notification and pushes it to an online recipient.
    ///
    /// When `forge.notification.email-enabled` or `sms-enabled` is set, the message is
    /// additionally delivered through the resolved e-mail address or phone number.
    ///
    /// @return the inbox notification
    @Transactional
    public NotificationMessage notifyUser(NotificationUserRequest request) {
        NotificationMessage message = new NotificationMessage(NotificationChannel.SYSTEM,
                request.userId(), request.title(), request.content(), request.type(),
                request.link(), request.actorId());
        repository.save(message);
        deliver(message);
        fanOut(request);
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

    /// Pushes notifications that were stored while the recipient was offline.
    ///
    /// @return number of pushed notifications
    @Transactional
    public int pushUndelivered(String userId) {
        if (publisher == null) {
            return 0;
        }
        List<NotificationMessage> undelivered = repository
                .findByRecipientAndChannelAndDeliveredAtIsNullOrderByIdAsc(userId,
                        NotificationChannel.SYSTEM, PageRequest.of(0, properties.batchSize()));
        int pushed = 0;
        for (NotificationMessage message : undelivered) {
            if (publisher.push(userId, message)) {
                message.markDelivered(Instant.now());
                pushed++;
            }
        }
        return pushed;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<NotificationMessage> listFor(String userId, boolean unreadOnly, int page, int size) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(Math.min(size, 100), 1);
        var pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        var result = unreadOnly
                ? repository.findByRecipientAndChannelAndReadAtIsNullOrderByIdDesc(
                userId, NotificationChannel.SYSTEM, pageable)
                : repository.findByRecipientAndChannelOrderByIdDesc(
                userId, NotificationChannel.SYSTEM, pageable);
        return PageResults.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return repository.countByRecipientAndChannelAndReadAtIsNull(userId, NotificationChannel.SYSTEM);
    }

    @Override
    @Transactional
    public void markRead(String userId, long notificationId) {
        requireOwned(userId, notificationId).markRead(Instant.now());
    }

    @Override
    @Transactional
    public int markAllRead(String userId) {
        List<NotificationMessage> unread = repository.findByRecipientAndChannelAndReadAtIsNull(
                userId, NotificationChannel.SYSTEM);
        Instant now = Instant.now();
        unread.forEach(message -> message.markRead(now));
        return unread.size();
    }

    @Override
    @Transactional
    public void delete(String userId, long notificationId) {
        repository.delete(requireOwned(userId, notificationId));
    }

    private NotificationMessage requireOwned(String userId, long notificationId) {
        return repository.findById(notificationId)
                .filter(message -> message.getChannel() == NotificationChannel.SYSTEM
                        && userId.equals(message.getRecipient()))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "Notification not found"));
    }

    private void fanOut(NotificationUserRequest request) {
        if (properties.emailEnabled()) {
            contacts.emailOf(request.userId()).ifPresent(address -> send(new NotificationRequest(
                    NotificationChannel.EMAIL, address, request.title(), request.content())));
        }
        if (properties.smsEnabled()) {
            contacts.phoneOf(request.userId()).ifPresent(phone -> send(new NotificationRequest(
                    NotificationChannel.SMS, phone, request.title(), request.content())));
        }
    }

    private void deliver(NotificationMessage message) {
        Optional<String> target = resolveTarget(message);
        if (target.isEmpty()) {
            message.markSkipped("No contact configured for recipient " + message.getRecipient());
            return;
        }
        message.retarget(target.get());
        NotificationSender sender = senders.get(message.getChannel());
        try {
            sender.send(message);
            message.markSent(Instant.now());
        } catch (RuntimeException ex) {
            LOG.warn("Notification [{}] via {} failed", message.getId(), message.getChannel(), ex);
            message.markFailed(ex.getMessage(), Instant.now(), properties.maxAttempts(), properties.retryDelay());
            return;
        }
        push(message);
    }

    private void push(NotificationMessage message) {
        if (publisher != null && message.getChannel() == NotificationChannel.SYSTEM
                && publisher.push(message.getRecipient(), message)) {
            message.markDelivered(Instant.now());
        }
    }

    /// Resolves the actual delivery target of the channel.
    ///
    /// Addresses passed in directly are used as they are; bare user identifiers are
    /// translated through the [NotificationContactResolver].
    private Optional<String> resolveTarget(NotificationMessage message) {
        String recipient = message.getRecipient();
        return switch (message.getChannel()) {
            case SYSTEM -> Optional.of(recipient);
            case EMAIL -> recipient.contains("@")
                    ? Optional.of(recipient)
                    : contacts.emailOf(recipient);
            case SMS -> recipient.matches("^\\+?\\d{6,15}$")
                    ? Optional.of(recipient)
                    : contacts.phoneOf(recipient);
        };
    }

    private Map<NotificationChannel, NotificationSender> resolve(List<NotificationSender> custom) {
        Map<NotificationChannel, NotificationSender> resolved = new EnumMap<>(NotificationChannel.class);
        resolved.put(NotificationChannel.SYSTEM, new SystemNotificationSender());
        resolved.put(NotificationChannel.EMAIL, new EmailNotificationSender());
        resolved.put(NotificationChannel.SMS, new SmsNotificationSender(smsSender));
        for (NotificationSender sender : custom) {
            resolved.put(sender.channel(), sender);
        }
        return Map.copyOf(resolved);
    }
}
