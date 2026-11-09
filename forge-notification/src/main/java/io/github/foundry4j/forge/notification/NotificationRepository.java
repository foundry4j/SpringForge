package io.github.foundry4j.forge.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/// Repository backing the notification log.
public interface NotificationRepository extends JpaRepository<NotificationMessage, Long> {

    /// Selects pending notifications whose retry time has come.
    List<NotificationMessage> findByStatusAndNextAttemptAtBeforeOrderByIdAsc(
            NotificationStatus status, Instant now, Pageable pageable);

    /// Counts notifications in the given state, useful for health reporting.
    long countByStatus(NotificationStatus status);
}
