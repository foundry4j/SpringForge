package io.github.foundry4j.forge.idempotency;

import io.github.foundry4j.forge.core.exception.ConflictException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;

/// Applies [Idempotent] around annotated methods and types.
///
/// Successful results are serialized and replayed for duplicates; failures release the key
/// so the caller can retry. The aspect runs after the rate limiting aspect.
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 300)
public class IdempotentAspect {

    private final IdempotentStore store;
    private final IdempotentKeyResolver keyResolver;
    private final ObjectMapper objectMapper;

    /// Creates the aspect.
    public IdempotentAspect(IdempotentStore store, IdempotentKeyResolver keyResolver, ObjectMapper objectMapper) {
        this.store = store;
        this.keyResolver = keyResolver;
        this.objectMapper = objectMapper;
    }

    /// Executes the method once per idempotency key.
    @Around("@annotation(io.github.foundry4j.forge.idempotency.Idempotent) || @within(io.github.foundry4j.forge.idempotency.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Idempotent idempotent = resolve(method, joinPoint);
        String key = keyResolver.resolve(idempotent, method, joinPoint.getTarget(), joinPoint.getArgs());
        IdempotentEntry entry = store.tryAcquire(key, idempotent.ttlSeconds());
        if (entry == null) {
            return proceedAndCache(joinPoint, method, key, idempotent.ttlSeconds());
        }
        if (entry.state() == IdempotentEntry.State.DONE) {
            return deserialize(entry.payload(), method);
        }
        throw new ConflictException("Duplicate request for idempotency key [%s]".formatted(key));
    }

    private Idempotent resolve(Method method, ProceedingJoinPoint joinPoint) {
        Idempotent annotation = method.getAnnotation(Idempotent.class);
        if (annotation != null) {
            return annotation;
        }
        Object target = joinPoint.getTarget();
        return (target == null ? method.getDeclaringClass() : target.getClass()).getAnnotation(Idempotent.class);
    }

    private Object proceedAndCache(ProceedingJoinPoint joinPoint, Method method, String key, long ttlSeconds)
            throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            store.markFailed(key);
            throw ex;
        }
        store.markDone(key, serialize(result, method), ttlSeconds);
        return result;
    }

    private byte[] serialize(Object result, Method method) {
        if (result == null || isVoid(method.getReturnType())) {
            return new byte[0];
        }
        return objectMapper.writeValueAsBytes(result);
    }

    private Object deserialize(byte[] payload, Method method) {
        if (payload == null || payload.length == 0 || isVoid(method.getReturnType())) {
            return null;
        }
        JavaType type = objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
        return objectMapper.readValue(payload, type);
    }

    private static boolean isVoid(Class<?> type) {
        return type == void.class || type == Void.class;
    }
}
