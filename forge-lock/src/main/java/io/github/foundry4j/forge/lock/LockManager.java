package io.github.foundry4j.forge.lock;

import io.github.foundry4j.forge.core.exception.Errors;

import java.time.Duration;
import java.util.function.Supplier;

/// Acquires and releases distributed locks.
public interface LockManager {

    /// Tries to acquire a lock, waiting at most `waitTime`.
    ///
    /// @param key       logical lock key
    /// @param waitTime  maximum time to block while the lock is held elsewhere
    /// @param leaseTime lease after which the lock expires automatically
    /// @return a handle; check [LockHandle#acquired()] before running the critical section
    LockHandle tryLock(String key, Duration waitTime, Duration leaseTime);

    /// Runs `action` while holding the lock and releases it afterwards.
    ///
    /// @throws io.github.foundry4j.forge.core.exception.BusinessException when the lock cannot be acquired
    default <T> T execute(String key, Duration waitTime, Duration leaseTime, Supplier<T> action) {
        try (LockHandle handle = tryLock(key, waitTime, leaseTime)) {
            if (!handle.acquired()) {
                throw Errors.locked(key);
            }
            return action.get();
        }
    }

    /// Runs `action` while holding the lock.
    default void execute(String key, Duration waitTime, Duration leaseTime, Runnable action) {
        execute(key, waitTime, leaseTime, () -> {
            action.run();
            return null;
        });
    }
}
