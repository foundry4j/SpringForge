package io.github.foundry4j.forge.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declarative rate limiting for methods and types.
///
/// ```java
/// @RateLimit(permits = 5, windowSeconds = 60, key = KeyStrategy.USER)
/// public void sendSmsCode() { ... }
/// ```
///
/// Calls exceeding [permits] inside [windowSeconds] raise
/// [io.github.foundry4j.forge.core.exception.TooManyRequestsException], which the web layer
/// renders as HTTP 429.
@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /// Maximum number of calls allowed inside the window.
    int permits();

    /// Window length in seconds.
    long windowSeconds() default 60L;

    /// Dimension used to isolate quotas.
    KeyStrategy key() default KeyStrategy.USER;
}
