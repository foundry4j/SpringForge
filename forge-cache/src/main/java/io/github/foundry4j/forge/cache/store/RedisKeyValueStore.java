package io.github.foundry4j.forge.cache.store;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/// Redis backed [KeyValueStore].
///
/// All operations that must be atomic are implemented as Lua scripts so that a
/// counter cannot be created without its expiry and a lock cannot be released by a
/// non owner.
public final class RedisKeyValueStore implements KeyValueStore {

    private static final RedisScript<Long> INCREMENT_WITH_TTL = RedisScript.of("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate template;
    private final String prefix;

    /// Creates a store writing keys below `prefix`.
    public RedisKeyValueStore(StringRedisTemplate template, String prefix) {
        this.template = template;
        this.prefix = prefix;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(template.opsForValue().get(prefix + key));
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        template.opsForValue().set(prefix + key, value, ttl);
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(template.opsForValue().setIfAbsent(prefix + key, value, ttl));
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = template.execute(INCREMENT_WITH_TTL, List.of(prefix + key), String.valueOf(ttl.toMillis()));
        return value == null ? 0L : value;
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(template.delete(prefix + key));
    }
}
