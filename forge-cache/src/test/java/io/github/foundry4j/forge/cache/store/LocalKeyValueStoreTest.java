package io.github.foundry4j.forge.cache.store;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the Caffeine backed key/value store.
class LocalKeyValueStoreTest {

    private final KeyValueStore store = new LocalKeyValueStore(100);

    @Test
    void storesReadsAndDeletesValues() {
        assertThat(store.get("missing")).isEmpty();
        store.set("k", "v", Duration.ofMinutes(1));
        assertThat(store.get("k")).contains("v");
        assertThat(store.exists("k")).isTrue();
        assertThat(store.delete("k")).isTrue();
        assertThat(store.exists("k")).isFalse();
        assertThat(store.delete("k")).isFalse();
    }

    @Test
    void setIfAbsentOnlyWritesOnce() {
        assertThat(store.setIfAbsent("k", "first", Duration.ofMinutes(1))).isTrue();
        assertThat(store.setIfAbsent("k", "second", Duration.ofMinutes(1))).isFalse();
        assertThat(store.get("k")).contains("first");
    }

    @Test
    void incrementsCounterAtomically() {
        assertThat(store.increment("counter", Duration.ofMinutes(1))).isEqualTo(1);
        assertThat(store.increment("counter", Duration.ofMinutes(1))).isEqualTo(2);
        assertThat(store.get("counter")).contains("2");
    }
}
