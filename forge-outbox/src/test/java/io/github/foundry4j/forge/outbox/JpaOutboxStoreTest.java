package io.github.foundry4j.forge.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests for the JPA outbox store.
class JpaOutboxStoreTest {

    private final OutboxRepository repository = mock(OutboxRepository.class);
    private final OutboxProperties properties = new OutboxProperties(
            true, true, 10, 3, Duration.ofSeconds(5), Duration.ofSeconds(1),
            Duration.ofMinutes(1), Duration.ofDays(1));
    private OutboxStore store;

    @BeforeEach
    void setUp() {
        store = new JpaOutboxStore(repository, properties);
    }

    @Test
    void enqueuesPendingEvents() {
        var occurredAt = Instant.parse("2026-01-01T00:00:00Z");

        store.enqueue("m-1", "order.created", "orders", "{}", "{}", "acme", occurredAt);

        var captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMessageId()).isEqualTo("m-1");
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(captor.getValue().getTenantId()).isEqualTo("acme");
        assertThat(captor.getValue().getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void claimsAndLocksPendingEvents() {
        var event = new OutboxEvent("m-1", "type", "", "{}", "{}", null, Instant.now());
        when(repository.lockPending(eq(OutboxStatus.PENDING), any(), any(Pageable.class)))
                .thenReturn(List.of(event));

        var claimed = store.claim(5);

        assertThat(claimed).hasSize(1);
        assertThat(claimed.getFirst().getStatus()).isEqualTo(OutboxStatus.PROCESSING);
    }

    @Test
    void marksEventsAsSentOrFailed() {
        var sent = new OutboxEvent("m-1", "type", "", "{}", "{}", null, Instant.now());
        when(repository.findById(1L)).thenReturn(Optional.of(sent));

        store.markSent(1L, Instant.now());

        assertThat(sent.getStatus()).isEqualTo(OutboxStatus.SENT);

        var failed = new OutboxEvent("m-2", "type", "", "{}", "{}", null, Instant.now());
        when(repository.findById(2L)).thenReturn(Optional.of(failed));

        store.markFailed(2L, "boom", Instant.now());

        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getLastError()).isEqualTo("boom");
    }

    @Test
    void recoversStuckEventsAndPurgesSentOnes() {
        store.recoverStuck(Duration.ofMinutes(5));
        store.purge(Duration.ofDays(7));

        verify(repository).recoverStuck(eq(OutboxStatus.PENDING), eq(OutboxStatus.PROCESSING),
                any(Instant.class), any(Instant.class));
        verify(repository).purgeSent(eq(OutboxStatus.SENT), any(Instant.class));
    }

    @Test
    void countsPendingEvents() {
        when(repository.countByStatus(OutboxStatus.PENDING)).thenReturn(3L);

        assertThat(store.pendingCount()).isEqualTo(3);
    }
}
