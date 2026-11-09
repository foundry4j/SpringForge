package io.github.foundry4j.forge.web.ratelimit;

import io.github.foundry4j.forge.cache.store.KeyValueStore;
import io.github.foundry4j.forge.core.exception.TooManyRequestsException;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

/// Applies [RateLimit] around annotated methods.
///
/// A fixed window counter is incremented atomically in the key/value store. When Redis
/// is configured the counter is shared by every node, otherwise it is local to the JVM.
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 300)
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate:";

    private final KeyValueStore store;
    private final ExpressionEvaluator evaluator;

    /// Creates the aspect.
    public RateLimitAspect(KeyValueStore store, ExpressionEvaluator evaluator) {
        this.store = store;
        this.evaluator = evaluator;
    }

    /// Counts the invocation and rejects it when the window is exhausted.
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String name = rateLimit.name().isBlank() ? method.getName() : rateLimit.name();
        String subject = resolveSubject(joinPoint, method, rateLimit);
        long windowMillis = Math.max(1L, rateLimit.windowSeconds()) * 1000L;
        long windowIndex = System.currentTimeMillis() / windowMillis;
        String key = KEY_PREFIX + name + ":" + subject + ":" + windowIndex;

        long current = store.increment(key, Duration.ofMillis(windowMillis + 1000L));
        if (current > rateLimit.permits()) {
            long elapsed = System.currentTimeMillis() % windowMillis;
            long retryAfter = Math.max(1L, (windowMillis - elapsed) / 1000L);
            throw new TooManyRequestsException(
                    "Rate limit [%s] exceeded, limit is %d per %ds"
                            .formatted(name, rateLimit.permits(), rateLimit.windowSeconds()),
                    retryAfter);
        }
        return joinPoint.proceed();
    }

    private String resolveSubject(ProceedingJoinPoint joinPoint, Method method, RateLimit rateLimit) {
        if (!rateLimit.key().isBlank()) {
            String value = evaluator.evaluate(rateLimit.key(), method, joinPoint.getTarget(), joinPoint.getArgs());
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return clientAddress();
    }

    private String clientAddress() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "local";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
