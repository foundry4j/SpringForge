package io.github.foundry4j.forge.security.annotation;

import io.github.foundry4j.forge.security.Logical;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Requires the caller to carry one or all of the declared roles.
///
/// ```java
/// @RequiresRole("ADMIN")
/// @PostMapping("/users")
/// public UserView create(@RequestBody CreateUserRequest request) { ... }
/// ```
///
/// An anonymous caller receives `401`, an authenticated caller without the role
/// receives `403`.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresRole {

    /// Required role codes.
    String[] value();

    /// Whether every role must be present.
    Logical logical() default Logical.ANY;
}
