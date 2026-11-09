package io.github.foundry4j.forge.cache.autoconfigure;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.foundry4j.forge.cache.CacheProvider;
import io.github.foundry4j.forge.cache.ForgeCacheErrorHandler;
import io.github.foundry4j.forge.cache.ForgeCacheProperties;
import io.github.foundry4j.forge.cache.TieredCacheManager;
import io.github.foundry4j.forge.cache.store.KeyValueStore;
import io.github.foundry4j.forge.cache.store.LocalKeyValueStore;
import io.github.foundry4j.forge.cache.store.RedisKeyValueStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/// Wires the cache layer and the [KeyValueStore] used by other forge modules.
@AutoConfiguration(before = CacheAutoConfiguration.class)
@EnableCaching
@EnableConfigurationProperties(ForgeCacheProperties.class)
public class ForgeCacheAutoConfiguration implements CachingConfigurer {

    private final CacheErrorHandler errorHandler = new ForgeCacheErrorHandler();

    /// Creates the key/value store matching the configured provider.
    @Bean
    @ConditionalOnMissingBean
    public KeyValueStore forgeKeyValueStore(ForgeCacheProperties properties,
                                            ObjectProvider<StringRedisTemplate> redisTemplate) {
        if (properties.provider() != CacheProvider.LOCAL) {
            StringRedisTemplate template = redisTemplate.getIfAvailable();
            if (template != null) {
                return new RedisKeyValueStore(template, properties.keyPrefix());
            }
        }
        return new LocalKeyValueStore(properties.localMaxSize());
    }

    /// Creates a cache manager when the application does not define one.
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager forgeCacheManager(ForgeCacheProperties properties,
                                          ObjectProvider<RedisConnectionFactory> connectionFactory,
                                          ObjectProvider<ObjectMapper> objectMapper) {
        RedisConnectionFactory factory = connectionFactory.getIfAvailable();
        if (properties.provider() == CacheProvider.LOCAL || factory == null) {
            var manager = new CaffeineCacheManager();
            manager.setCaffeine(Caffeine.newBuilder()
                    .maximumSize(properties.localMaxSize())
                    .expireAfterWrite(properties.localTtl()));
            return manager;
        }
        ObjectMapper mapper = objectMapper.getIfAvailable(() -> JsonMapper.builder().build());
        RedisCacheManager redisManager = redisCacheManager(factory, properties, mapper);
        return properties.provider() == CacheProvider.TIERED
                ? new TieredCacheManager(properties, redisManager)
                : redisManager;
    }

    /// Keeps the application alive when Redis is temporarily unavailable.
    @Override
    public CacheErrorHandler errorHandler() {
        return errorHandler;
    }

    private RedisCacheManager redisCacheManager(RedisConnectionFactory factory,
                                                ForgeCacheProperties properties,
                                                ObjectMapper mapper) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.remoteTtl())
                .computePrefixWith(name -> properties.keyPrefix() + name + ":")
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJacksonJsonRedisSerializer(mapper)));
        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .transactionAware()
                .build();
    }
}
