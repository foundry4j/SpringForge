package io.github.foundry4j.forge.idempotency;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the in-memory idempotency store.
class InMemoryIdempotentStoreTest {

    private final InMemoryIdempotentStore store = new InMemoryIdempotentStore();

    @Test
    void reservesTheKeyForTheFirstCaller() {
        assertThat(store.tryAcquire("k", 60)).isNull();
        assertThat(store.tryAcquire("k", 60).state()).isEqualTo(IdempotentEntry.State.PROCESSING);
    }

    @Test
    void storesTheResultAsDone() {
        assertThat(store.tryAcquire("k", 60)).isNull();
        store.markDone("k", "ok".getBytes(StandardCharsets.UTF_8), 60);

        IdempotentEntry entry = store.tryAcquire("k", 60);

        assertThat(entry.state()).isEqualTo(IdempotentEntry.State.DONE);
        assertThat(new String(entry.payload(), StandardCharsets.UTF_8)).isEqualTo("ok");
    }

    @Test
    void releasesFailedKeys() {
        assertThat(store.tryAcquire("k", 60)).isNull();
        store.markFailed("k");

        assertThat(store.tryAcquire("k", 60)).isNull();
    }

    @Test
    void expiresReservations() throws InterruptedException {
        assertThat(store.tryAcquire("k", 1)).isNull();
        Thread.sleep(1_100);

        assertThat(store.tryAcquire("k", 1)).isNull();
    }
}
