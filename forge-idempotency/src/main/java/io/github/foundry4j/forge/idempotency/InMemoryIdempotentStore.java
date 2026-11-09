package io.github.foundry4j.forge.idempotency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// In-memory [IdempotentStore] for single node deployments.
///
/// Expired entries are replaced lazily on the next `tryAcquire` for the same key.
public class InMemoryIdempotentStore implements IdempotentStore {

    private final Map<String, IdempotentEntry> entries = new ConcurrentHashMap<>();

    @Override
    public IdempotentEntry tryAcquire(String key, long ttlSeconds) {
        long now = System.currentTimeMillis();
        long expiresAt = now + Math.max(1L, ttlSeconds) * 1000L;
        IdempotentEntry[] existing = new IdempotentEntry[1];
        entries.compute(key, (_, current) -> {
            if (current == null || current.expiresAt() <= now) {
                existing[0] = null;
                return new IdempotentEntry(IdempotentEntry.State.PROCESSING, null, expiresAt);
            }
            existing[0] = current;
            return current;
        });
        return existing[0];
    }

    @Override
    public void markDone(String key, byte[] payload, long ttlSeconds) {
        long expiresAt = System.currentTimeMillis() + Math.max(1L, ttlSeconds) * 1000L;
        entries.computeIfPresent(key,
                (_, _) -> new IdempotentEntry(IdempotentEntry.State.DONE, payload, expiresAt));
    }

    @Override
    public void markFailed(String key) {
        entries.remove(key);
    }
}
