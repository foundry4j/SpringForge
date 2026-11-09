package io.github.foundry4j.forge.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;
import java.util.List;

/// [org.springframework.cache.CacheManager] producing [TieredCache] instances on demand.
public final class TieredCacheManager extends AbstractCacheManager {

    private final ForgeCacheProperties properties;
    private final RedisCacheManager redisCacheManager;

    /// Creates a manager that combines Caffeine and Redis.
    public TieredCacheManager(ForgeCacheProperties properties, RedisCacheManager redisCacheManager) {
        this.properties = properties;
        this.redisCacheManager = redisCacheManager;
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return List.of();
    }

    @Override
    protected Cache getMissingCache(String name) {
        var caffeine = Caffeine.newBuilder()
                .maximumSize(properties.localMaxSize())
                .expireAfterWrite(properties.localTtl())
                .build();
        Cache local = new CaffeineCache(name, caffeine);
        Cache remote = redisCacheManager.getCache(name);
        return new TieredCache(name, local, remote);
    }
}
