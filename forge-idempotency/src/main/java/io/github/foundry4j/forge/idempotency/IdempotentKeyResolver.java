package io.github.foundry4j.forge.idempotency;

import java.lang.reflect.Method;

/// Strategy used to derive an idempotency key.
///
/// The default implementation is [ExpressionIdempotentKeyResolver]. Applications can
/// declare their own bean, for example to read a message header or a security context.
@FunctionalInterface
public interface IdempotentKeyResolver {

    /// Resolves the key for the intercepted invocation.
    ///
    /// @param config annotation declaring the key
    /// @param method intercepted method
    /// @param target target instance
    /// @param args   invocation arguments
    /// @return the idempotency key
    String resolve(Idempotent config, Method method, Object target, Object[] args);
}
