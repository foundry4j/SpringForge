package io.github.foundry4j.forge.web.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Rejects duplicated invocations of the same operation.
///
/// The key is taken from the configured header, or from the SpEL `key` expression when
/// it is not blank. The key is remembered for [ttlSeconds] seconds; a second request
/// carrying the same key fails with HTTP 409.
///
/// ```java
/// @Idempotent(key = "#request.orderNo", ttlSeconds = 600)
/// public void create(CreateOrderRequest request) { ... }
/// ```
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /// SpEL expression producing the key; takes precedence over [header] when set.
    String key() default "";

    /// Header carrying the idempotency key.
    String header() default "Idempotency-Key";

    /// How long the key is remembered, in seconds.
    long ttlSeconds() default 300L;

    /// Prefix used to namespace the key in the underlying store.
    String prefix() default "idempotent:";
}
