package io.github.foundry4j.forge.outbox;

import io.github.foundry4j.forge.messaging.MessageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Tests for the background outbox relay.
class OutboxRelayTest {

    private final OutboxStore store = mock(OutboxStore.class);
    private final OutboxTransport transport = mock(OutboxTransport.class);
    private final MessageFactory factory = new MessageFactory(JsonMapper.builder().build());
    private final OutboxProperties properties = new OutboxProperties(
            true, true, 10, 3, Duration.ofMillis(10), Duration.ofSeconds(1),
            Duration.ofMinutes(1), Duration.ofDays(1));
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(store, transport, factory, properties);
    }

    @Test
    void marksDeliveredEventsAsSent() {
        when(store.claim(10)).thenReturn(List.of(event(1L)));
        when(store.recoverStuck(any())).thenReturn(0);
        when(store.purge(any())).thenReturn(0);

        relay.relayOnce();

        verify(transport).deliver(any());
        verify(store).markSent(eq(1L), any(Instant.class));
    }

    @Test
    void marksEventsAsFailedWhenDeliveryThrows() {
        when(store.claim(10)).thenReturn(List.of(event(2L)));
        doThrow(new IllegalStateException("broker down")).when(transport).deliver(any());

        relay.relayOnce();

        verify(store).markFailed(eq(2L), contains("broker down"), any(Instant.class));
        verify(store, never()).markSent(anyLong(), any());
    }

    @Test
    void doesNothingWhenNoEventIsPending() {
        when(store.claim(10)).thenReturn(List.of());

        relay.relayOnce();

        verifyNoInteractions(transport);
        verify(store, never()).markSent(anyLong(), any());
    }

    @Test
    void recoversAndPurgesBeforeClaiming() {
        when(store.recoverStuck(any())).thenReturn(2);
        when(store.purge(any())).thenReturn(1);
        when(store.claim(10)).thenReturn(List.of());

        relay.relayOnce();

        var ordered = inOrder(store);
        ordered.verify(store).recoverStuck(properties.stuckTimeout());
        ordered.verify(store).purge(properties.retention());
        ordered.verify(store).claim(10);
    }

    @Test
    void survivesStoreFailures() {
        when(store.recoverStuck(any())).thenThrow(new IllegalStateException("db down"));

        relay.relayOnce();

        verify(store, never()).claim(anyInt());
    }

    private static OutboxEvent event(long id) {
        var event = new OutboxEvent("m-" + id, "order.created", "orders", "{}",
                "{\"traceId\":\"t-1\"}", "acme", Instant.now());
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
