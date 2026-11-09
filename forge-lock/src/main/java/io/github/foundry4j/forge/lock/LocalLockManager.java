package io.github.foundry4j.forge.lock;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/// JVM local [LockManager].
///
/// The implementation keeps one [ReentrantLock] per key and removes it again once no
/// thread holds or waits for it, so the map cannot grow without bound.
public final class LocalLockManager implements LockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public LockHandle tryLock(String key, Duration waitTime, Duration leaseTime) {
        ReentrantLock lock = locks.computeIfAbsent(key, _ -> new ReentrantLock());
        try {
            if (!lock.tryLock(Math.max(waitTime.toMillis(), 0L), TimeUnit.MILLISECONDS)) {
                return LockHandle.unacquired(key);
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return LockHandle.unacquired(key);
        }
        return new LocalLockHandle(key, lock);
    }

    /// Handle releasing the local reentrant lock and cleaning up the registry entry.
    private final class LocalLockHandle implements LockHandle {

        private final String key;
        private final ReentrantLock lock;
        private boolean released;

        private LocalLockHandle(String key, ReentrantLock lock) {
            this.key = key;
            this.lock = lock;
        }

        @Override
        public String key() {
            return key;
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
            lock.unlock();
            if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                locks.remove(key, lock);
            }
        }
    }
}
