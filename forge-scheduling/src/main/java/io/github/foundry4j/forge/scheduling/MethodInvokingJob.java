package io.github.foundry4j.forge.scheduling;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/// Job that invokes a no-argument method of a Spring bean.
///
/// This is the bridge between the [ForgeScheduled] annotation and Quartz: the bean name
/// and the method name travel in the job data map, while the bean itself is resolved
/// from the application context at execution time so that proxies and lazy beans keep
/// working.
public class MethodInvokingJob implements Job {

    /// Job data key holding the target bean name.
    public static final String TARGET_BEAN = "forge.targetBean";

    /// Job data key holding the target method name.
    public static final String TARGET_METHOD = "forge.targetMethod";

    private final ApplicationContext context;

    /// Creates the job; instances are produced by [ForgeJobFactory].
    public MethodInvokingJob(ApplicationContext context) {
        this.context = context;
    }

    /// {@inheritDoc}
    @Override
    public void execute(JobExecutionContext executionContext) throws JobExecutionException {
        JobDataMap data = executionContext.getMergedJobDataMap();
        String beanName = data.getString(TARGET_BEAN);
        String methodName = data.getString(TARGET_METHOD);
        try {
            Object bean = context.getBean(beanName);
            Method method = ClassUtils.getMethodIfAvailable(AopUtils.getTargetClass(bean), methodName);
            if (method == null) {
                throw new NoSuchMethodException("%s#%s".formatted(beanName, methodName));
            }
            ReflectionUtils.makeAccessible(method);
            method.invoke(bean);
        } catch (ReflectiveOperationException exception) {
            throw new JobExecutionException(exception);
        }
    }
}
