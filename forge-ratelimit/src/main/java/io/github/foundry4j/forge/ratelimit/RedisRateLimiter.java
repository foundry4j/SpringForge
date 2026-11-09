package io.github.foundry4j.forge.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/// Redis backed [RateLimiter] that shares one fixed window across all instances.
///
/// The check, the increment and the expiry are executed by a single Lua script, so two
/// nodes cannot both observe the last permit. The window starts with the first call and
/// every key expires on its own, which keeps Redis free of stale counters.
public final class RedisRateLimiter implements RateLimiter {

    private static final RedisScript<Long> ACQUIRE = RedisScript.of("""
            local permits = tonumber(ARGV[2])
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current >= permits then
                return 0
            end
            current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate template;
    private final String prefix;

    /// Creates the limiter writing counters below `prefix`.
    public RedisRateLimiter(StringRedisTemplate template, String prefix) {
        this.template = template;
        this.prefix = prefix;
    }

    @Override
    public boolean tryAcquire(String key, int permits, long windowSeconds) {
        long windowMillis = Math.max(1L, windowSeconds) * 1000L;
        Long allowed = template.execute(ACQUIRE,
                List.of(prefix + key),
                String.valueOf(windowMillis),
                String.valueOf(Math.max(1, permits)));
        return allowed != null && allowed == 1L;
    }
}
