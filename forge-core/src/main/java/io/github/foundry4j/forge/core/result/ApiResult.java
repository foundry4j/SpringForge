package io.github.foundry4j.forge.core.result;

import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.core.error.ErrorCode;

import java.time.Instant;

/// Uniform envelope for successful responses.
///
/// The web module wraps plain controller return values into this record so that
/// every endpoint answers with the same shape. Errors are rendered as RFC 9457
/// problem details by the global exception handler.
///
/// @param code      business code, `0` for success
/// @param message   human readable message
/// @param data      payload, may be `null`
/// @param traceId   correlation identifier of the current request
/// @param timestamp creation time
public record ApiResult<T>(
        String code,
        String message,
        T data,
        String traceId,
        Instant timestamp
) {

    /// Code used for successful responses.
    public static final String SUCCESS_CODE = "0";

    /// Creates a successful response carrying a payload.
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(SUCCESS_CODE, "OK", data, currentTraceId(), Instant.now());
    }

    /// Creates a successful response without a payload.
    public static ApiResult<Void> ok() {
        return ok(null);
    }

    /// Creates a failed response; the HTTP status is decided by the caller.
    public static <T> ApiResult<T> failure(ErrorCode errorCode, String message) {
        return new ApiResult<>(errorCode.code(), message, null, currentTraceId(), Instant.now());
    }

    /// Whether this response represents a successful operation.
    public boolean successful() {
        return SUCCESS_CODE.equals(code);
    }

    private static String currentTraceId() {
        return ForgeContext.current().map(ForgeContext.Caller::traceId).orElse(null);
    }
}
