package io.github.foundry4j.forge.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/// Registers every [ForgeScheduled] method once the application context is ready.
///
/// Scanning happens on `ContextRefreshedEvent` and uses the bean definition types, so no
/// bean is instantiated just to be inspected for annotations. Each method is registered as
/// a JobRunr recurring job that dispatches back to the bean through [ForgeJobDispatcher].
public class ForgeScheduledRegistrar implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ForgeScheduledRegistrar.class);

    private final ForgeScheduler scheduler;
    private final ForgeSchedulingProperties properties;
    private final AtomicBoolean registered = new AtomicBoolean();

    /// Creates the registrar.
    public ForgeScheduledRegistrar(ForgeScheduler scheduler, ForgeSchedulingProperties properties) {
        this.scheduler = scheduler;
        this.properties = properties;
    }

    /// {@inheritDoc}
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!(event.getApplicationContext() instanceof ConfigurableApplicationContext context)) {
            return;
        }
        if (!registered.compareAndSet(false, true)) {
            return;
        }
        ForgeJobDispatcher.bind(context);
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        for (String beanName : beanFactory.getBeanNamesForType(Object.class)) {
            Class<?> beanType = beanFactory.getType(beanName);
            if (beanType == null) {
                continue;
            }
            registerMethods(beanName, beanType);
        }
    }

    private void registerMethods(String beanName, Class<?> beanType) {
        Map<Method, ForgeScheduled> annotated = MethodIntrospector.selectMethods(beanType,
                (MethodIntrospector.MetadataLookup<ForgeScheduled>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, ForgeScheduled.class));
        annotated.forEach((method, scheduled) -> register(beanName, method, scheduled));
    }

    private void register(String beanName, Method method, ForgeScheduled scheduled) {
        if (method.getParameterCount() > 0) {
            LOG.warn("Ignoring @ForgeScheduled method [{}.{}]: only no-argument methods are supported",
                    beanName, method.getName());
            return;
        }
        String name = scheduled.name().isBlank() ? beanName + "." + method.getName() : scheduled.name();
        String group = scheduled.group().isBlank() ? properties.group() : scheduled.group();
        try {
            if (!scheduled.cron().isBlank()) {
                scheduler.scheduleCron(name, group, scheduled.cron(),
                        () -> ForgeJobDispatcher.invoke(beanName, method.getName()));
                LOG.info("Registered cron job [{}] from {}.{}", name, beanName, method.getName());
            } else if (scheduled.fixedDelay() > 0) {
                scheduler.scheduleFixedDelay(name, group, Duration.ofMillis(scheduled.fixedDelay()),
                        () -> ForgeJobDispatcher.invoke(beanName, method.getName()));
                LOG.info("Registered fixed delay job [{}] from {}.{}", name, beanName, method.getName());
            } else {
                LOG.warn("Ignoring @ForgeScheduled method [{}.{}]: cron and fixedDelay are both empty",
                        beanName, method.getName());
            }
        } catch (RuntimeException exception) {
            LOG.error("Cannot register @ForgeScheduled method [{}.{}]", beanName, method.getName(), exception);
        }
    }
}
