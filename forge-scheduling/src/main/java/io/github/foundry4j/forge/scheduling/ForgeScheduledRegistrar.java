package io.github.foundry4j.forge.scheduling;

import org.quartz.Job;
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
/// bean is instantiated just to be inspected for annotations.
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
        Map<String, Object> data = Map.of(
                MethodInvokingJob.TARGET_BEAN, beanName,
                MethodInvokingJob.TARGET_METHOD, method.getName());
        Class<? extends Job> jobType = scheduled.concurrent()
                ? MethodInvokingJob.class
                : DisallowConcurrentMethodInvokingJob.class;
        try {
            if (!scheduled.cron().isBlank()) {
                scheduler.scheduleCron(name, group, jobType, scheduled.cron(), data);
                LOG.info("Registered cron job [{}] from {}.{}", name, beanName, method.getName());
            } else if (scheduled.fixedDelay() > 0) {
                scheduler.scheduleFixedDelay(name, group, jobType,
                        Duration.ofMillis(scheduled.fixedDelay()), data);
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
