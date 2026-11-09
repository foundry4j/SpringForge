package io.github.foundry4j.forge.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests for the two level cache.
class TieredCacheTest {

    private final Cache local = new ConcurrentMapCache("local");
    private final Cache remote = new ConcurrentMapCache("remote");

    @Test
    void readsThroughBothTiersAndPromotesRemoteHits() {
        var cache = new TieredCache("orders", local, remote);
        remote.put("k", "remote-value");

        assertThat(cache.get("k", String.class)).isEqualTo("remote-value");
        assertThat(local.get("k", String.class)).isEqualTo("remote-value");

        cache.put("k2", "v2");
        assertThat(remote.get("k2", String.class)).isEqualTo("v2");

        cache.evict("k2");
        assertThat(remote.get("k2")).isNull();

        cache.clear();
        assertThat(local.get("k")).isNull();
    }

    @Test
    void loadsMissingValuesOnce() {
        var cache = new TieredCache("orders", local, null);
        var loads = new AtomicInteger();

        assertThat(cache.get("k", () -> {
            loads.incrementAndGet();
            return "loaded";
        })).isEqualTo("loaded");
        assertThat(cache.get("k", () -> {
            loads.incrementAndGet();
            return "loaded";
        })).isEqualTo("loaded");
        assertThat(loads).hasValue(1);
    }

    @Test
    void swallowsRemoteFailures() {
        Cache broken = mock(Cache.class);
        when(broken.get("k")).thenThrow(new IllegalStateException("redis down"));
        var cache = new TieredCache("orders", local, broken);

        assertThat(cache.get("k")).isNull();
        cache.put("k", "v");
        cache.evict("k");
        cache.clear();
        assertThat(cache.get("k")).isNull();
    }

    @Test
    void putIfAbsentReturnsExistingValue() {
        var cache = new TieredCache("orders", local, remote);
        assertThat(cache.putIfAbsent("k", "first")).isNull();

        var existing = cache.putIfAbsent("k", "second");
        assertThat(existing).isNotNull();
        assertThat(existing.get()).isEqualTo("first");
    }
}
