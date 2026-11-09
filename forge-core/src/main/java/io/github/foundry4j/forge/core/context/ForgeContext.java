package io.github.foundry4j.forge.core.context;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/// Request scoped state shared by every forge module.
///
/// The context is stored in a [ScopedValue] instead of a `ThreadLocal`. That keeps
/// the state immutable for the duration of a request and works naturally with
/// virtual threads: when a request is handled on a virtual thread the binding is
/// visible to every callee without copying anything.
///
/// ```java
/// ForgeContext.runAs(new ForgeContext.Caller("acme", "42", "trace"), () -> service.execute());
/// ```
public final class ForgeContext {

    /// Immutable snapshot of the caller of the current operation.
    ///
    /// @param tenantId tenant the request belongs to
    /// @param userId   authenticated user, may be `null` for anonymous traffic
    /// @param traceId  correlation identifier used in logs and error responses
    public record Caller(String tenantId, String userId, String traceId) {

        /// Replaces blank values with `null` so that the context stays canonical.
        public Caller {
            tenantId = blankToNull(tenantId);
            userId = blankToNull(userId);
            traceId = blankToNull(traceId);
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }

    private static final ScopedValue<Caller> CURRENT = ScopedValue.newInstance();

    private ForgeContext() {
    }

    /// Returns the caller of the current operation or an empty optional when the
    /// current thread is not bound to a request.
    public static Optional<Caller> current() {
        return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
    }

    /// Returns the tenant of the current operation.
    public static Optional<String> tenantId() {
        return current().map(Caller::tenantId).filter(Objects::nonNull);
    }

    /// Returns the authenticated user of the current operation.
    public static Optional<String> userId() {
        return current().map(Caller::userId).filter(Objects::nonNull);
    }

    /// Returns the trace identifier used for correlation.
    public static String traceId() {
        return current().map(Caller::traceId).orElse("-");
    }

    /// Returns the caller or throws when no context is bound.
    public static Caller requireCaller() {
        return current().orElseThrow(() -> new IllegalStateException(
                "No ForgeContext is bound to the current thread; wrap the call in ForgeContext.runAs(...)"));
    }

    /// Creates a scoped value carrier so a servlet filter can bind the context
    /// around the whole request chain.
    public static ScopedValue.Carrier carrier(Caller caller) {
        return ScopedValue.where(CURRENT, caller);
    }

    /// Runs an action inside the given caller scope.
    public static void runAs(Caller caller, Runnable action) {
        ScopedValue.where(CURRENT, caller).run(action);
    }

    /// Calls an action inside the given caller scope and propagates checked exceptions.
    public static <T> T callAs(Caller caller, Callable<T> action) throws Exception {
        return ScopedValue.where(CURRENT, caller).call(action::call);
    }
}
