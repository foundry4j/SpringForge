package io.github.foundry4j.forge.lock.autoconfigure;

import io.github.foundry4j.forge.cache.autoconfigure.ForgeCacheAutoConfiguration;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import io.github.foundry4j.forge.lock.ForgeLockProperties;
import io.github.foundry4j.forge.lock.LocalLockManager;
import io.github.foundry4j.forge.lock.LockManager;
import io.github.foundry4j.forge.lock.LockProvider;
import io.github.foundry4j.forge.lock.RedisLockManager;
import io.github.foundry4j.forge.lock.aspect.DistributedLockAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/// Wires the lock manager and its aspect.
@AutoConfiguration(after = ForgeCacheAutoConfiguration.class)
@ConditionalOnProperty(prefix = "forge.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ForgeLockProperties.class)
public class ForgeLockAutoConfiguration {

    /// Creates a Redis based manager when requested and available, otherwise a local one.
    @Bean
    @ConditionalOnMissingBean
    public LockManager forgeLockManager(ForgeLockProperties properties,
                                        ObjectProvider<StringRedisTemplate> redisTemplate) {
        if (properties.provider() == LockProvider.REDIS) {
            StringRedisTemplate template = redisTemplate.getIfAvailable();
            if (template != null) {
                return new RedisLockManager(template, properties.keyPrefix(),
                        properties.retryInterval(), properties.leaseTime());
            }
        }
        return new LocalLockManager();
    }

    /// Registers the annotation driven aspect.
    @Bean
    @ConditionalOnMissingBean
    public DistributedLockAspect forgeDistributedLockAspect(LockManager lockManager,
                                                            ForgeLockProperties properties,
                                                            ExpressionEvaluator evaluator) {
        return new DistributedLockAspect(lockManager, properties, evaluator);
    }
}
