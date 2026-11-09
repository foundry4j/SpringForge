package io.github.foundry4j.forge.core.exception;

import io.github.foundry4j.forge.core.error.ErrorCode;

/// Base class for all failures that are expected by the application.
///
/// The global exception handler translates [BusinessException] into a RFC 9457
/// problem detail carrying [ErrorCode#code()] so that clients never have to parse
/// free text.
public class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;

    /// Creates an exception using the default message of the given error code.
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    /// Creates an exception with a custom, human readable message.
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /// Creates an exception with a custom message and an underlying cause.
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /// Stable error descriptor exposed to clients.
    public ErrorCode errorCode() {
        return errorCode;
    }
}
