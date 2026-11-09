package io.github.foundry4j.forge.ratelimit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies the Redis limiter contract and the key layout it writes.
class RedisRateLimiterTest {

    @Test
    void allowsCallsUntilTheScriptRejectsThem() {
        assertThat(limiterReturning(1L).tryAcquire("rl:key", 5, 60)).isTrue();
        assertThat(limiterReturning(0L).tryAcquire("rl:key", 5, 60)).isFalse();
        assertThat(limiterReturning(null).tryAcquire("rl:key", 5, 60)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsTheWindowAndTheQuotaToRedis() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(1L);

        new RedisRateLimiter(template, "forge:ratelimit:").tryAcquire("rl:key", 7, 30);

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object> window = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> permits = ArgumentCaptor.forClass(Object.class);
        verify(template).execute(any(RedisScript.class), keys.capture(), window.capture(), permits.capture());
        assertThat(keys.getValue()).containsExactly("forge:ratelimit:rl:key");
        assertThat(window.getValue()).isEqualTo("30000");
        assertThat(permits.getValue()).isEqualTo("7");
    }

    @SuppressWarnings("unchecked")
    private static RedisRateLimiter limiterReturning(Long result) {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(result);
        return new RedisRateLimiter(template, "forge:ratelimit:");
    }
}
