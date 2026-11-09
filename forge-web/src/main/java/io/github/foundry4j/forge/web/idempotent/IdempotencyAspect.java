package io.github.foundry4j.forge.web.idempotent;

import io.github.foundry4j.forge.cache.store.KeyValueStore;
import io.github.foundry4j.forge.core.exception.ConflictException;
import io.github.foundry4j.forge.core.exception.Errors;
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

/// Applies [Idempotent] around annotated methods.
///
/// The key is written before the method runs and removed again when the method
/// fails, so a client can safely retry after a transient error while concurrent
/// duplicates are rejected.
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class IdempotencyAspect {

    private final KeyValueStore store;
    private final ExpressionEvaluator evaluator;

    /// Creates the aspect.
    public IdempotencyAspect(KeyValueStore store, ExpressionEvaluator evaluator) {
        this.store = store;
        this.evaluator = evaluator;
    }

    /// Guards the annotated method.
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String rawKey = resolveKey(joinPoint, method, idempotent);
        if (rawKey == null || rawKey.isBlank()) {
            throw Errors.badRequest("Missing idempotency key in header [%s]".formatted(idempotent.header()));
        }
        String key = idempotent.prefix() + scope(method) + ":" + rawKey;
        boolean first = store.setIfAbsent(key, "1", Duration.ofSeconds(idempotent.ttlSeconds()));
        if (!first) {
            throw new ConflictException("Duplicate request for idempotency key [%s]".formatted(rawKey));
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            store.delete(key);
            throw ex;
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, Method method, Idempotent idempotent) {
        if (!idempotent.key().isBlank()) {
            return evaluator.evaluate(idempotent.key(), method, joinPoint.getTarget(), joinPoint.getArgs());
        }
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader(idempotent.header());
    }

    private String scope(Method method) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return method.toGenericString();
        }
        return request.getMethod() + " " + request.getRequestURI();
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
