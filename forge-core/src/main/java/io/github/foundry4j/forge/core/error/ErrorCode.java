package io.github.foundry4j.forge.core.error;

/// Contract for a stable, machine readable error descriptor.
///
/// Implementations are usually enums so that every failure raised by the
/// framework or by an application owns a stable code that clients can branch on.
///
/// ```java
/// public enum OrderError implements ErrorCode {
///     ORDER_LOCKED("ORDER_001", "Order is locked", 409);
/// }
/// ```
public interface ErrorCode {

    /// Unique, stable code exposed to clients, for example `COMMON_404`.
    String code();

    /// Human readable default message; it may be overridden per exception instance.
    String message();

    /// HTTP status that best describes the failure, for example `404`.
    int httpStatus();
}
