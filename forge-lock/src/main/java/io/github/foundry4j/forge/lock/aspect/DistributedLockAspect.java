package io.github.foundry4j.forge.lock.aspect;

import io.github.foundry4j.forge.core.exception.Errors;
import io.github.foundry4j.forge.core.support.Durations;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import io.github.foundry4j.forge.lock.ForgeLockProperties;
import io.github.foundry4j.forge.lock.LockHandle;
import io.github.foundry4j.forge.lock.LockManager;
import io.github.foundry4j.forge.lock.annotation.DistributedLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.time.Duration;

/// Applies [DistributedLock] around annotated methods.
///
/// The order is lower than the transaction interceptor order, so the lock is
/// acquired before a transaction starts and released after it committed.
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class DistributedLockAspect {

    private final LockManager lockManager;
    private final ForgeLockProperties properties;
    private final ExpressionEvaluator evaluator;

    /// Creates the aspect.
    public DistributedLockAspect(LockManager lockManager,
                                 ForgeLockProperties properties,
                                 ExpressionEvaluator evaluator) {
        this.lockManager = lockManager;
        this.properties = properties;
        this.evaluator = evaluator;
    }

    /// Acquires the lock, invokes the method and releases the lock.
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String key = evaluator.evaluate(distributedLock.key(), method, joinPoint.getTarget(), joinPoint.getArgs());
        if (key == null || key.isBlank()) {
            throw Errors.badRequest("Distributed lock key must not be blank");
        }
        Duration waitTime = Durations.parse(distributedLock.waitTime(), properties.waitTime());
        Duration leaseTime = Durations.parse(distributedLock.leaseTime(), properties.leaseTime());
        try (LockHandle handle = lockManager.tryLock(key, waitTime, leaseTime)) {
            if (!handle.acquired()) {
                throw Errors.locked(key);
            }
            return joinPoint.proceed();
        }
    }
}
