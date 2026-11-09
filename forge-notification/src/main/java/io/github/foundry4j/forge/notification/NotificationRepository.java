package io.github.foundry4j.forge.notification;

import io.github.foundry4j.forge.data.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

/// Repository backing the notification log and the in-application inbox.
public interface NotificationRepository extends BaseRepository<NotificationMessage> {

    /// Selects pending notifications whose retry time has come.
    List<NotificationMessage> findByStatusAndNextAttemptAtBeforeOrderByIdAsc(
            NotificationStatus status, Instant now, Pageable pageable);

    /// Counts notifications in the given state, useful for health reporting.
    long countByStatus(NotificationStatus status);

    /// Inbox page of one recipient, newest first.
    Page<NotificationMessage> findByRecipientAndChannelOrderByIdDesc(
            String recipient, NotificationChannel channel, Pageable pageable);

    /// Unread inbox page of one recipient, newest first.
    Page<NotificationMessage> findByRecipientAndChannelAndReadAtIsNullOrderByIdDesc(
            String recipient, NotificationChannel channel, Pageable pageable);

    /// Unread notifications of one recipient.
    List<NotificationMessage> findByRecipientAndChannelAndReadAtIsNull(
            String recipient, NotificationChannel channel);

    /// Number of unread notifications of one recipient.
    long countByRecipientAndChannelAndReadAtIsNull(String recipient, NotificationChannel channel);

    /// Notifications that were persisted but never pushed to an open session.
    List<NotificationMessage> findByRecipientAndChannelAndDeliveredAtIsNullOrderByIdAsc(
            String recipient, NotificationChannel channel, Pageable pageable);
}
