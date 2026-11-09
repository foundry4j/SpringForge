package io.github.foundry4j.forge.scheduling;

import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/// Invokes a bean method registered by [ForgeScheduledRegistrar].
///
/// JobRunr persists jobs as a class, a method and serialized arguments. The target bean
/// itself must not be serialized, so the registrar stores the bean name and the method
/// name and delegates to this static dispatcher, which resolves the bean from the
/// application context of the node that executes the job.
public final class ForgeJobDispatcher {

    private static final AtomicReference<ApplicationContext> CONTEXT = new AtomicReference<>();

    private ForgeJobDispatcher() {
    }

    /// Binds the dispatcher to the application context of the current node.
    public static void bind(ApplicationContext context) {
        CONTEXT.set(context);
    }

    /// Invokes the no-argument method `methodName` of the bean `beanName`.
    ///
    /// Any failure is rethrown so JobRunr marks the job as failed and applies its retry
    /// policy.
    public static void invoke(String beanName, String methodName) {
        ApplicationContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("ForgeJobDispatcher is not bound to an application context");
        }
        Object bean = context.getBean(beanName);
        Method method = ClassUtils.getMethodIfAvailable(bean.getClass(), methodName);
        if (method == null) {
            throw new IllegalStateException(
                    "Scheduled method [%s.%s] no longer exists".formatted(beanName, methodName));
        }
        ReflectionUtils.makeAccessible(method);
        try {
            method.invoke(bean);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(
                    "Scheduled method [%s.%s] failed".formatted(beanName, methodName),
                    exception.getTargetException());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Cannot invoke scheduled method [%s.%s]".formatted(beanName, methodName), exception);
        }
    }
}
