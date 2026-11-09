package io.github.foundry4j.forge.cache.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/// In-process [KeyValueStore] backed by Caffeine.
///
/// Caffeine supports variable expiration, so every entry can carry its own time to
/// live without maintaining a background reaper.
public final class LocalKeyValueStore implements KeyValueStore {

    private final Cache<String, String> cache;

    /// Creates a store bounded by `maximumSize`.
    public LocalKeyValueStore(long maximumSize) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        cache.policy().expireVariably().ifPresentOrElse(
                expiration -> expiration.put(key, value, ttl),
                () -> cache.put(key, value));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        var previous = cache.asMap().putIfAbsent(key, value);
        if (previous != null) {
            return false;
        }
        set(key, value, ttl);
        return true;
    }

    @Override
    public long increment(String key, Duration ttl) {
        var counter = new AtomicLong();
        cache.asMap().compute(key, (_, current) -> {
            long next = current == null ? 1L : Long.parseLong(current) + 1L;
            counter.set(next);
            return Long.toString(next);
        });
        if (counter.get() == 1L) {
            set(key, "1", ttl);
        }
        return counter.get();
    }

    @Override
    public boolean delete(String key) {
        return cache.asMap().remove(key) != null;
    }
}
