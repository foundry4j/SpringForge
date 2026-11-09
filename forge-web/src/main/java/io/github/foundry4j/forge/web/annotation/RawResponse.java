package io.github.foundry4j.forge.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Skips the uniform result envelope for a controller or a single handler.
///
/// File downloads, third party callbacks and endpoints that speak a foreign protocol
/// must return their payload unchanged, so [io.github.foundry4j.forge.web.result.ApiResponseAdvice]
/// ignores anything carrying this annotation.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RawResponse {
}
