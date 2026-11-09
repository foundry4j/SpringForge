package io.github.foundry4j.forge.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the in-memory fixed window limiter.
class InMemoryRateLimiterTest {

    private final InMemoryRateLimiter limiter = new InMemoryRateLimiter();

    @Test
    void allowsPermitsAndRejectsTheNextCall() {
        assertThat(limiter.tryAcquire("k", 2, 60)).isTrue();
        assertThat(limiter.tryAcquire("k", 2, 60)).isTrue();
        assertThat(limiter.tryAcquire("k", 2, 60)).isFalse();
    }

    @Test
    void keepsIndependentCountersPerKey() {
        assertThat(limiter.tryAcquire("a", 1, 60)).isTrue();
        assertThat(limiter.tryAcquire("b", 1, 60)).isTrue();
        assertThat(limiter.tryAcquire("a", 1, 60)).isFalse();
    }

    @Test
    void resetsAfterTheWindow() throws InterruptedException {
        assertThat(limiter.tryAcquire("w", 1, 1)).isTrue();
        assertThat(limiter.tryAcquire("w", 1, 1)).isFalse();
        Thread.sleep(1_100);
        assertThat(limiter.tryAcquire("w", 1, 1)).isTrue();
    }
}
