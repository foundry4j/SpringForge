package io.github.foundry4j.forge.lock;

import io.github.foundry4j.forge.core.support.Ids;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

/// Redis based [LockManager] built on `SET key token NX PX lease`.
///
/// The release path runs a Lua script that compares the owner token before deleting,
/// which prevents a slow caller from releasing a lock that already expired and was
/// re-acquired by someone else.
public final class RedisLockManager implements LockManager {

    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate template;
    private final String prefix;
    private final Duration retryInterval;
    private final Duration defaultLeaseTime;

    /// Creates the manager.
    ///
    /// @param template         Redis template used for the lock keys
    /// @param prefix           prefix applied to every lock key
    /// @param retryInterval    pause between two acquisition attempts
    /// @param defaultLeaseTime lease used when the caller does not provide one
    public RedisLockManager(StringRedisTemplate template,
                            String prefix,
                            Duration retryInterval,
                            Duration defaultLeaseTime) {
        this.template = template;
        this.prefix = prefix;
        this.retryInterval = retryInterval;
        this.defaultLeaseTime = defaultLeaseTime;
    }

    @Override
    public LockHandle tryLock(String key, Duration waitTime, Duration leaseTime) {
        String redisKey = prefix + key;
        String token = Ids.uuid();
        Duration lease = leaseTime == null || leaseTime.isZero() ? defaultLeaseTime : leaseTime;
        long deadline = System.nanoTime() + waitTime.toNanos();
        while (true) {
            if (Boolean.TRUE.equals(template.opsForValue().setIfAbsent(redisKey, token, lease))) {
                return new RedisLockHandle(redisKey, token);
            }
            if (System.nanoTime() - deadline >= 0) {
                return LockHandle.unacquired(key);
            }
            try {
                Thread.sleep(retryInterval.toMillis());
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return LockHandle.unacquired(key);
            }
        }
    }

    /// Handle that only deletes the Redis key when it still carries its own token.
    private final class RedisLockHandle implements LockHandle {

        private final String redisKey;
        private final String token;
        private boolean released;

        private RedisLockHandle(String redisKey, String token) {
            this.redisKey = redisKey;
            this.token = token;
        }

        @Override
        public String key() {
            return redisKey;
        }

        @Override
        public boolean acquired() {
            return !released;
        }

        @Override
        public void close() {
            if (released) {
                return;
            }
            released = true;
            template.execute(RELEASE_SCRIPT, List.of(redisKey), token);
        }
    }
}
