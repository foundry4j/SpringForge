package io.github.foundry4j.forge.lock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the in-process lock manager.
class LocalLockManagerTest {

    private final LockManager manager = new LocalLockManager();

    @Test
    void acquiresAndReleasesTheLock() {
        try (LockHandle handle = manager.tryLock("order-1", Duration.ZERO, Duration.ofSeconds(5))) {
            assertThat(handle.acquired()).isTrue();
            assertThat(handle.key()).isEqualTo("order-1");
        }
        try (LockHandle reacquired = manager.tryLock("order-1", Duration.ZERO, Duration.ofSeconds(5))) {
            assertThat(reacquired.acquired()).isTrue();
        }
    }

    @Test
    void rejectsConcurrentOwner() throws Exception {
        var holder = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var future = CompletableFuture.runAsync(() -> {
            try (LockHandle _ = manager.tryLock("order-1", Duration.ZERO, Duration.ofSeconds(5))) {
                holder.countDown();
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(holder.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            try (LockHandle handle = manager.tryLock("order-1", Duration.ofMillis(50), Duration.ofSeconds(5))) {
                assertThat(handle.acquired()).isFalse();
            }
        } finally {
            release.countDown();
            future.get(5, TimeUnit.SECONDS);
        }
    }
}
