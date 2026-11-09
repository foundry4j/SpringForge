package io.github.foundry4j.forge.lock.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Runs a method while holding a distributed lock.
///
/// The key is a SpEL expression evaluated against the method arguments:
///
/// ```java
/// @DistributedLock(key = "'order:' + #id")
/// public void cancel(Long id) { ... }
/// ```
///
/// The aspect wraps the method, so the lock is released even when the method throws.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /// SpEL expression producing the lock key.
    String key();

    /// Maximum time to wait for the lock, for example `2s`; empty uses the global default.
    String waitTime() default "";

    /// Lease after which the lock expires automatically, for example `30s`.
    String leaseTime() default "";
}
