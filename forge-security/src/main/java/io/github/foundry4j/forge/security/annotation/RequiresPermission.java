package io.github.foundry4j.forge.security.annotation;

import io.github.foundry4j.forge.security.Logical;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Requires the caller to carry one or all of the declared permissions.
///
/// ```java
/// @RequiresPermission("order:refund")
/// @PostMapping("/orders/{id}/refund")
/// public OrderView refund(@PathVariable long id) { ... }
/// ```
///
/// An anonymous caller receives `401`, an authenticated caller without the
/// permission receives `403`.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresPermission {

    /// Required permission codes.
    String[] value();

    /// Whether every permission must be present.
    Logical logical() default Logical.ANY;
}
