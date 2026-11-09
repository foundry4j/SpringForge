package io.github.foundry4j.forge.ratelimit;

import java.lang.reflect.Method;

/// Resolves the value a [KeyStrategy] is keyed by.
///
/// The module has no web dependency: applications without an HTTP request get the
/// `local` subject, while `forge-web` reads the authenticated user or the client
/// address from the current request.
@FunctionalInterface
public interface RateLimitSubjectResolver {

    /// Resolves the subject for the given strategy.
    ///
    /// @param strategy dimension declared on the annotation
    /// @param method   intercepted method
    /// @param target   target instance
    /// @param args     invocation arguments
    /// @return the subject, never `null`
    String resolve(KeyStrategy strategy, Method method, Object target, Object[] args);
}
