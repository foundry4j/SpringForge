package io.github.foundry4j.forge.idempotency;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies the Redis idempotency store, including the value encoding it persists.
class RedisIdempotentStoreTest {

    private static final String PREFIX = "forge:idempotency:";

    @Test
    void reportsTheFirstCallAsUnreserved() {
        assertThat(storeReturning(null).tryAcquire("order", 60)).isNull();
    }

    @Test
    void reportsAnEntryStillBeingProcessed() {
        IdempotentEntry entry = storeReturning("P").tryAcquire("order", 60);

        assertThat(entry).isNotNull();
        assertThat(entry.state()).isEqualTo(IdempotentEntry.State.PROCESSING);
        assertThat(entry.payload()).isNull();
    }

    @Test
    void decodesACachedResult() {
        byte[] payload = new byte[]{1, 2, 3};
        String stored = "D:" + Base64.getEncoder().encodeToString(payload);

        IdempotentEntry entry = storeReturning(stored).tryAcquire("order", 60);

        assertThat(entry).isNotNull();
        assertThat(entry.state()).isEqualTo(IdempotentEntry.State.DONE);
        assertThat(entry.payload()).containsExactly(payload);
    }

    @Test
    @SuppressWarnings("unchecked")
    void writesTheEncodedResultWithItsTtl() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        byte[] payload = new byte[]{4, 5, 6};

        new RedisIdempotentStore(template, PREFIX).markDone("order", payload, 90);

        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> ttl = ArgumentCaptor.forClass(Object.class);
        verify(template).execute(any(RedisScript.class), eq(List.of(PREFIX + "order")),
                value.capture(), ttl.capture());
        assertThat(value.getValue()).isEqualTo("D:" + Base64.getEncoder().encodeToString(payload));
        assertThat(ttl.getValue()).isEqualTo("90000");
    }

    @Test
    void releasesTheKeyAfterAFailure() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);

        new RedisIdempotentStore(template, PREFIX).markFailed("order");

        verify(template).delete(PREFIX + "order");
    }

    @SuppressWarnings("unchecked")
    private static RedisIdempotentStore storeReturning(String result) {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(result);
        return new RedisIdempotentStore(template, PREFIX);
    }
}
