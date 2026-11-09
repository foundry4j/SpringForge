package io.github.foundry4j.forge.resilience;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Adds a circuit breaker, retries and an optional fallback to a method.
///
/// The fallback must live on the same class, accept the same parameters and return a
/// compatible type. It may additionally declare a trailing [Throwable] parameter to
/// inspect the failure that triggered the fallback.
///
/// ```java
/// @FaultTolerant(name = "inventory", fallback = "fallbackStock")
/// public int stock(long sku) { ... }
///
/// private int fallbackStock(long sku, Throwable cause) { return 0; }
/// ```
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FaultTolerant {

    /// Circuit breaker and retry name; defaults to the method name.
    String name() default "";

    /// Name of the fallback method on the same class; empty means no fallback.
    String fallback() default "";
}
