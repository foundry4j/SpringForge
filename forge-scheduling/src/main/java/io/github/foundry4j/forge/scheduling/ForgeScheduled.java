package io.github.foundry4j.forge.scheduling;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares a no-argument bean method as a recurring JobRunr job.
///
/// Either `cron` or `fixedDelay` must be set. The job is registered when the application
/// context is refreshed and can be cancelled through [ForgeScheduler].
///
/// ```java
/// @ForgeScheduled(cron = "0 */5 * * * *", name = "order-expiry")
/// void expireOrders() {
///     orderService.expireOverdue();
/// }
/// ```
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ForgeScheduled {

    /// JobRunr cron expression, five or six fields, for example `0 */5 * * * *`.
    String cron() default "";

    /// Fixed interval between two executions, in milliseconds.
    long fixedDelay() default -1;

    /// Job name, defaults to `<beanName>.<methodName>`.
    String name() default "";

    /// Job group used as the identifier prefix, defaults to `forge.scheduling.group`.
    String group() default "";
}
