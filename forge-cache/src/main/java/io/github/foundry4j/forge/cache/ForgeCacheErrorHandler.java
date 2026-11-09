package io.github.foundry4j.forge.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/// Cache error handler that degrades gracefully instead of failing the business call.
public final class ForgeCacheErrorHandler implements CacheErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ForgeCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        LOG.warn("Cache read failed [{}:{}]: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        LOG.warn("Cache write failed [{}:{}]: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        LOG.warn("Cache evict failed [{}:{}]: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        LOG.warn("Cache clear failed [{}]: {}", cache.getName(), exception.getMessage());
    }
}
