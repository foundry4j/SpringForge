package io.github.foundry4j.forge.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;

/// Two level cache combining a Caffeine tier with a Redis tier.
///
/// Reads hit the local tier first, then Redis. Writes go to both tiers. Redis
/// failures are logged and swallowed so that a cache outage degrades latency but
/// never breaks the business flow.
public final class TieredCache implements Cache {

    private static final Logger LOG = LoggerFactory.getLogger(TieredCache.class);

    private final String name;
    private final Cache local;
    private final Cache remote;

    /// Creates a tiered cache.
    ///
    /// @param name   logical cache name
    /// @param local  in-process tier
    /// @param remote shared tier, may be `null` when Redis is unavailable
    public TieredCache(String name, Cache local, Cache remote) {
        this.name = name;
        this.local = local;
        this.remote = remote;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return local.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper = local.get(key);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = remoteGet(key);
        if (wrapper != null) {
            local.put(key, wrapper.get());
        }
        return wrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = local.get(key, type);
        if (value != null) {
            return value;
        }
        value = remoteGet(key, type);
        if (value != null) {
            local.put(key, value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(Object key, Object value) {
        local.put(key, value);
        remoteRun(() -> remote.put(key, value));
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        local.evict(key);
        remoteRun(() -> remote.evict(key));
    }

    @Override
    public void clear() {
        local.clear();
        remoteRun(remote::clear);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean evicted = local.evictIfPresent(key);
        remoteRun(() -> remote.evictIfPresent(key));
        return evicted;
    }

    @Override
    public boolean invalidate() {
        boolean invalidated = local.invalidate();
        remoteRun(remote::invalidate);
        return invalidated;
    }

    private ValueWrapper remoteGet(Object key) {
        if (remote == null) {
            return null;
        }
        try {
            return remote.get(key);
        } catch (RuntimeException ex) {
            LOG.warn("Redis read failed for cache [{}]: {}", name, ex.getMessage());
            return null;
        }
    }

    private <T> T remoteGet(Object key, Class<T> type) {
        if (remote == null) {
            return null;
        }
        try {
            return remote.get(key, type);
        } catch (RuntimeException ex) {
            LOG.warn("Redis read failed for cache [{}]: {}", name, ex.getMessage());
            return null;
        }
    }

    private void remoteRun(Runnable action) {
        if (remote == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException ex) {
            LOG.warn("Redis write failed for cache [{}]: {}", name, ex.getMessage());
        }
    }

    /// Convenience wrapper used when callers only have a plain value.
    public ValueWrapper wrap(Object value) {
        return new SimpleValueWrapper(value);
    }
}
