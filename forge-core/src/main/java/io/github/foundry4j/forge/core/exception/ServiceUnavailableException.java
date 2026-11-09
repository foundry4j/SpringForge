package io.github.foundry4j.forge.core.exception;

import io.github.foundry4j.forge.core.error.CommonErrorCode;

/// Raised when a downstream dependency is unavailable, or a circuit breaker is open.
public final class ServiceUnavailableException extends BusinessException {

    /// Creates a failure using the default "service unavailable" message.
    public ServiceUnavailableException() {
        super(CommonErrorCode.SERVICE_UNAVAILABLE);
    }

    /// Creates a failure with a custom message.
    public ServiceUnavailableException(String message) {
        super(CommonErrorCode.SERVICE_UNAVAILABLE, message);
    }

    /// Creates a failure with a custom message and cause.
    public ServiceUnavailableException(String message, Throwable cause) {
        super(CommonErrorCode.SERVICE_UNAVAILABLE, message, cause);
    }
}
