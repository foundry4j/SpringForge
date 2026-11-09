package io.github.foundry4j.forge.ratelimit;

import io.github.foundry4j.forge.core.exception.TooManyRequestsException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/// Applies [RateLimit] around annotated methods and types.
///
/// The key follows `rl:{class}#{method}:{strategy}:{subject}`, so every operation gets
/// an independent quota. The aspect runs before the idempotency aspect, matching the
/// reference implementation: flood protection first, duplicate detection second.
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class RateLimitAspect {

    private static final RateLimitSubjectResolver LOCAL = (strategy, method, target, args) -> "local";

    private final RateLimiter rateLimiter;
    private final RateLimitSubjectResolver resolver;

    /// Creates the aspect with the local subject resolver.
    public RateLimitAspect(RateLimiter rateLimiter) {
        this(rateLimiter, LOCAL);
    }

    /// Creates the aspect with a custom subject resolver.
    public RateLimitAspect(RateLimiter rateLimiter, RateLimitSubjectResolver resolver) {
        this.rateLimiter = rateLimiter;
        this.resolver = resolver;
    }

    /// Counts the invocation and rejects it when the quota is exhausted.
    @Around("@annotation(io.github.foundry4j.forge.ratelimit.RateLimit) || @within(io.github.foundry4j.forge.ratelimit.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RateLimit rateLimit = resolve(method, joinPoint);
        if (!rateLimiter.tryAcquire(buildKey(rateLimit, method, joinPoint), rateLimit.permits(), rateLimit.windowSeconds())) {
            throw new TooManyRequestsException(
                    "Rate limit exceeded for [%s#%s]".formatted(method.getDeclaringClass().getSimpleName(), method.getName()),
                    rateLimit.windowSeconds());
        }
        return joinPoint.proceed();
    }

    private RateLimit resolve(Method method, ProceedingJoinPoint joinPoint) {
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        if (annotation != null) {
            return annotation;
        }
        Object target = joinPoint.getTarget();
        return (target == null ? method.getDeclaringClass() : target.getClass()).getAnnotation(RateLimit.class);
    }

    private String buildKey(RateLimit rateLimit, Method method, ProceedingJoinPoint joinPoint) {
        String base = "rl:" + method.getDeclaringClass().getName() + "#" + method.getName();
        return switch (rateLimit.key()) {
            case GLOBAL -> base + ":global";
            case USER -> base + ":user:" + resolve(KeyStrategy.USER, method, joinPoint);
            case IP -> base + ":ip:" + resolve(KeyStrategy.IP, method, joinPoint);
        };
    }

    private String resolve(KeyStrategy strategy, Method method, ProceedingJoinPoint joinPoint) {
        return resolver.resolve(strategy, method, joinPoint.getTarget(), joinPoint.getArgs());
    }
}
