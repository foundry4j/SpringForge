package io.github.foundry4j.forge.resilience;

import io.github.foundry4j.forge.core.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/// Applies [FaultTolerant] around annotated methods.
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 200)
public class FaultTolerantAspect {

    private static final Logger LOG = LoggerFactory.getLogger(FaultTolerantAspect.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /// Creates the aspect.
    public FaultTolerantAspect(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    /// Decorates the invocation with a circuit breaker, retries and the fallback.
    @Around("@annotation(faultTolerant)")
    public Object around(ProceedingJoinPoint joinPoint, FaultTolerant faultTolerant) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String name = faultTolerant.name().isBlank() ? method.getName() : faultTolerant.name();
        Retry retry = retryRegistry.retry(name);
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        var decorated = Retry.decorateCheckedSupplier(retry,
                CircuitBreaker.decorateCheckedSupplier(circuitBreaker, joinPoint::proceed));
        try {
            return decorated.get();
        } catch (CallNotPermittedException ex) {
            LOG.warn("Circuit [{}] is open, falling back to [{}]", name, faultTolerant.fallback());
            return fallback(joinPoint, faultTolerant, ex);
        } catch (Throwable ex) {
            LOG.warn("Call protected by [{}] failed: {}", name, ex.getMessage());
            return fallback(joinPoint, faultTolerant, ex);
        }
    }

    private Object fallback(ProceedingJoinPoint joinPoint, FaultTolerant faultTolerant, Throwable cause) throws Throwable {
        if (faultTolerant.fallback().isBlank()) {
            if (cause instanceof CallNotPermittedException) {
                throw new ServiceUnavailableException("Circuit is open", cause);
            }
            throw cause;
        }
        Object target = joinPoint.getTarget();
        Class<?> targetClass = AopUtils.getTargetClass(target);
        Class<?>[] parameterTypes = ((MethodSignature) joinPoint.getSignature()).getParameterTypes();
        Method fallback = findFallback(targetClass, faultTolerant.fallback(), parameterTypes);
        if (fallback == null) {
            throw new IllegalStateException(
                    "Fallback [%s] not found on [%s]".formatted(faultTolerant.fallback(), targetClass.getName()), cause);
        }
        Object[] arguments = fallback.getParameterCount() == parameterTypes.length + 1
                ? append(joinPoint.getArgs(), cause)
                : joinPoint.getArgs();
        ReflectionUtils.makeAccessible(fallback);
        try {
            return fallback.invoke(target, arguments);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    private Method findFallback(Class<?> targetClass, String name, Class<?>[] parameterTypes) {
        Method exact = ReflectionUtils.findMethod(targetClass, name, parameterTypes);
        if (exact != null) {
            return exact;
        }
        return ReflectionUtils.findMethod(targetClass, name, append(parameterTypes, Throwable.class));
    }

    private static Object[] append(Object[] arguments, Object last) {
        Object[] result = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, result, 0, arguments.length);
        result[arguments.length] = last;
        return result;
    }

    private static Class<?>[] append(Class<?>[] types, Class<?> last) {
        Class<?>[] result = new Class<?>[types.length + 1];
        System.arraycopy(types, 0, result, 0, types.length);
        result[types.length] = last;
        return result;
    }
}
