package io.github.foundry4j.forge.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declarative idempotency for methods and types.
///
/// ```java
/// @Idempotent(key = "order:#request.orderNo", ttlSeconds = 600)
/// public OrderView create(CreateOrderRequest request) { ... }
/// ```
///
/// The first call executes the method and caches its result for [ttlSeconds]. A duplicate
/// call received while the first one is still running fails with a conflict, and a
/// duplicate received afterwards returns the cached result without invoking the method
/// again.
///
/// `key` supports `#parameter.field` placeholders as well as `#p0` by index. When it is
/// blank the key is derived from the serialized arguments.
@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /// Key expression such as `order:#request.orderNo`; blank derives the key from the arguments.
    String key() default "";

    /// How long the result is cached, in seconds.
    long ttlSeconds() default 86_400L;
}
