package io.github.foundry4j.forge.core.exception;

import io.github.foundry4j.forge.core.error.CommonErrorCode;

/// Raised when a request conflicts with the current state of a resource,
/// for example a duplicated idempotency key or a unique constraint violation.
public final class ConflictException extends BusinessException {

    /// Creates a conflict failure with a free form message.
    public ConflictException(String message) {
        super(CommonErrorCode.CONFLICT, message);
    }
}
