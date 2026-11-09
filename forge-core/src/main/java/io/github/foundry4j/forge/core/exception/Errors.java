package io.github.foundry4j.forge.core.exception;

import io.github.foundry4j.forge.core.error.CommonErrorCode;

/// Small factory for the exceptions that are thrown most often.
///
/// The factory keeps call sites short while still producing exceptions with a
/// stable [io.github.foundry4j.forge.core.error.ErrorCode].
public final class Errors {

    private Errors() {
    }

    /// Creates a [NotFoundException] for a resource and identifier.
    public static NotFoundException notFound(String resource, Object id) {
        return new NotFoundException(resource, id);
    }

    /// Creates a [ConflictException].
    public static ConflictException conflict(String message) {
        return new ConflictException(message);
    }

    /// Creates a [BusinessException] describing an invalid request.
    public static BusinessException badRequest(String message) {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, message);
    }

    /// Creates a [BusinessException] describing a forbidden operation.
    public static BusinessException forbidden(String message) {
        return new BusinessException(CommonErrorCode.FORBIDDEN, message);
    }

    /// Creates a [BusinessException] describing a lock that could not be acquired.
    public static BusinessException locked(String key) {
        return new BusinessException(CommonErrorCode.LOCKED, "Failed to acquire lock [%s]".formatted(key));
    }
}
