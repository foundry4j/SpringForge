package io.github.foundry4j.forge.outbox;

import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.messaging.MessageFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/// Tests for the transactional outbox recorder.
class OutboxRecorderTest {

    private final OutboxStore store = mock(OutboxStore.class);
    private final MessageFactory factory = new MessageFactory(JsonMapper.builder().build());
    private final OutboxRecorder recorder = new OutboxRecorder(store, factory);

    @Test
    void recordsEventsWithTraceAndTenantHeaders() throws Exception {
        String id = ForgeContext.callAs(new ForgeContext.Caller("acme", "user-1", "trace-9"),
                () -> recorder.record("order.created", "orders.created", new Payload("A-1"), Map.of()));

        var headers = ArgumentCaptor.forClass(String.class);
        verify(store).enqueue(eq(id), eq("order.created"), eq("orders.created"), contains("A-1"),
                headers.capture(), eq("acme"), any());

        assertThat(factory.decodeHeaders(headers.getValue()))
                .containsEntry("traceId", "trace-9")
                .containsEntry("tenantId", "acme");
    }

    @Test
    void recordsEventsWithoutDestinationOrContext() {
        String id = recorder.record("ping", new Payload("B-2"));

        verify(store).enqueue(eq(id), eq("ping"), eq(""), anyString(), anyString(), isNull(), any());
    }

    @Test
    void recordsPrebuiltMessages() {
        var message = factory.create("order.paid", "orders", new Payload("C-3"), Map.of());

        assertThat(recorder.record(message)).isEqualTo(message.id());
        verify(store).enqueue(eq(message.id()), eq("order.paid"), eq("orders"), anyString(), anyString(),
                isNull(), any());
    }

    /// Payload used by the recorder tests.
    record Payload(String orderNo) {
    }
}
