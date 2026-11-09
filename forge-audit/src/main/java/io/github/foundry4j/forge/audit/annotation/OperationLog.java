package io.github.foundry4j.forge.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Records every invocation of the annotated method.
///
/// ```java
/// @OperationLog("refund-order")
/// public OrderView refund(long orderId) { ... }
/// ```
///
/// The aspect writes arguments, result or failure, duration, trace identifier and
/// client address into `forge_operation_log`. The row is written in its own
/// transaction, so a failed business call is recorded as well.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OperationLog {

    /// Business action name, defaults to the method name.
    String value() default "";

    /// Whether the arguments are serialized into the log.
    boolean recordArguments() default true;

    /// Whether the result is serialized into the log.
    boolean recordResult() default true;
}
