package io.github.foundry4j.forge.lock;

/// Handle for an acquired lock; closing it releases the lock.
///
/// The handle keeps the ownership token, which is required by Redis to make sure a
/// lock can never be released by a thread that does not own it.
public interface LockHandle extends AutoCloseable {

    /// Key the lock was acquired for.
    String key();

    /// Whether the lock is currently held.
    boolean acquired();

    /// Releases the lock. Calling it more than once is a no-op.
    @Override
    void close();

    /// Creates a handle representing a failed acquisition.
    static LockHandle unacquired(String key) {
        return new Unacquired(key);
    }

    /// Handle returned when the lock could not be acquired within the wait time.
    record Unacquired(String key) implements LockHandle {

        @Override
        public boolean acquired() {
            return false;
        }

        @Override
        public void close() {
            // nothing to release
        }
    }
}
