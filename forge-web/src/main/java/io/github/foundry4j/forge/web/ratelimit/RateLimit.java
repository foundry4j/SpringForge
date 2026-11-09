package io.github.foundry4j.forge.web.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Limits how often a method may be invoked inside a fixed window.
///
/// The limit is keyed by the SpEL expression when provided, otherwise by the client
/// IP address, and is shared through [io.github.foundry4j.forge.cache.store.KeyValueStore].
///
/// ```java
/// @RateLimit(name = "order-create", permits = 20, windowSeconds = 60)
/// public void create(CreateOrderRequest request) { ... }
/// ```
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /// Name of the limit, defaults to the method name.
    String name() default "";

    /// Maximum number of calls allowed inside the window.
    int permits() default 100;

    /// Window length in seconds.
    int windowSeconds() default 60;

    /// Optional SpEL expression identifying the caller, for example `#userId`.
    String key() default "";
}
