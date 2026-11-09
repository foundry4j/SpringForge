package io.github.foundry4j.forge.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Base64;
import java.util.List;

/// Redis backed [IdempotentStore] that shares state across all instances.
///
/// Reserving a key and completing it are both single Lua scripts, so two nodes cannot
/// both run the same request: the loser observes `PROCESSING` and fails with a conflict.
/// Expiry is delegated to Redis; the `expiresAt` returned by [tryAcquire] is therefore an
/// upper bound, not an exact deadline.
public final class RedisIdempotentStore implements IdempotentStore {

    private static final String PROCESSING = "P";
    private static final String DONE_PREFIX = "D:";

    private static final RedisScript<String> ACQUIRE = RedisScript.of("""
            local current = redis.call('GET', KEYS[1])
            if current == false then
                redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
                return false
            end
            return current
            """, String.class);

    private static final RedisScript<Long> COMPLETE = RedisScript.of("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate template;
    private final String prefix;

    /// Creates the store writing keys below `prefix`.
    public RedisIdempotentStore(StringRedisTemplate template, String prefix) {
        this.template = template;
        this.prefix = prefix;
    }

    @Override
    public IdempotentEntry tryAcquire(String key, long ttlSeconds) {
        long ttlMillis = Math.max(1L, ttlSeconds) * 1000L;
        String value = template.execute(ACQUIRE, List.of(prefix + key),
                PROCESSING, String.valueOf(ttlMillis));
        if (value == null) {
            return null;
        }
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        if (value.startsWith(DONE_PREFIX)) {
            return new IdempotentEntry(IdempotentEntry.State.DONE, decode(value), expiresAt);
        }
        return new IdempotentEntry(IdempotentEntry.State.PROCESSING, null, expiresAt);
    }

    @Override
    public void markDone(String key, byte[] payload, long ttlSeconds) {
        long ttlMillis = Math.max(1L, ttlSeconds) * 1000L;
        byte[] body = payload == null ? new byte[0] : payload;
        String value = DONE_PREFIX + Base64.getEncoder().encodeToString(body);
        template.execute(COMPLETE, List.of(prefix + key), value, String.valueOf(ttlMillis));
    }

    @Override
    public void markFailed(String key) {
        template.delete(prefix + key);
    }

    private static byte[] decode(String value) {
        String encoded = value.substring(DONE_PREFIX.length());
        return encoded.isEmpty() ? new byte[0] : Base64.getDecoder().decode(encoded);
    }
}
