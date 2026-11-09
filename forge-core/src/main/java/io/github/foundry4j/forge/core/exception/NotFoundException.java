package io.github.foundry4j.forge.core.exception;

import io.github.foundry4j.forge.core.error.CommonErrorCode;

/// Raised when an addressed resource cannot be found.
public final class NotFoundException extends BusinessException {

    /// Creates a "not found" failure with a free form message.
    public NotFoundException(String message) {
        super(CommonErrorCode.NOT_FOUND, message);
    }

    /// Creates a "not found" failure for a concrete resource and identifier.
    public NotFoundException(String resource, Object id) {
        super(CommonErrorCode.NOT_FOUND, "%s[%s] was not found".formatted(resource, id));
    }
}
