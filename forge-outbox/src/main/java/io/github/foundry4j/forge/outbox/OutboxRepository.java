package io.github.foundry4j.forge.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/// Repository backing the transactional outbox.
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /// Selects pending events and locks them so two relay instances never fight for
    /// the same row.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from OutboxEvent e
            where e.status = :status and e.nextAttemptAt <= :now
            order by e.id asc
            """)
    List<OutboxEvent> lockPending(@Param("status") OutboxStatus status,
                                  @Param("now") Instant now,
                                  Pageable pageable);

    /// Returns abandoned `PROCESSING` rows to the pending state.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OutboxEvent e
            set e.status = :pending, e.updatedAt = :now, e.nextAttemptAt = :now
            where e.status = :processing and e.updatedAt < :threshold
            """)
    int recoverStuck(@Param("pending") OutboxStatus pending,
                     @Param("processing") OutboxStatus processing,
                     @Param("threshold") Instant threshold,
                     @Param("now") Instant now);

    /// Deletes delivered events older than the retention window.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from OutboxEvent e where e.status = :status and e.sentAt < :threshold")
    int purgeSent(@Param("status") OutboxStatus status, @Param("threshold") Instant threshold);

    /// Counts events in the given state, useful for health reporting.
    long countByStatus(OutboxStatus status);
}
