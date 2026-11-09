package io.github.foundry4j.forge.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Fixed window rate limiter kept in memory.
///
/// Suitable for single node deployments. Windows expire lazily: an entry is reset the
/// next time the same key is seen. Multi instance deployments should provide a
/// distributed [RateLimiter] bean backed by Redis.
public class InMemoryRateLimiter implements RateLimiter {

    private static final class Window {

        private final long startMillis;
        private int count;

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int permits, long windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1L, windowSeconds) * 1000L;
        Window window = windows.compute(key, (_, current) -> {
            if (current == null || now - current.startMillis >= windowMillis) {
                Window fresh = new Window(now);
                fresh.count = 1;
                return fresh;
            }
            current.count++;
            return current;
        });
        return window.count <= permits;
    }
}
