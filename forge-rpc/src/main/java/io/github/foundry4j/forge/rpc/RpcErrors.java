package io.github.foundry4j.forge.rpc;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.error.ErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import java.util.Optional;

/// Maps business error codes to gRPC status codes and back.
///
/// Server side, a [BusinessException] is translated into the closest gRPC status and its
/// stable error code is written to the `x-error-code` trailer. Client side,
/// [#toBusinessException] restores the original [BusinessException] so callers keep
/// branching on the same error codes they use for HTTP.
public final class RpcErrors {

    /// Trailer carrying the stable business error code.
    public static final Metadata.Key<String> ERROR_CODE_KEY =
            Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER);

    private RpcErrors() {
    }

    /// Converts any throwable into a gRPC status.
    public static Status toStatus(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException statusRuntimeException) {
            return statusRuntimeException.getStatus();
        }
        if (throwable instanceof StatusException statusException) {
            return statusException.getStatus();
        }
        if (throwable instanceof BusinessException business) {
            return Status.fromCode(toStatusCode(business.errorCode().httpStatus()))
                    .withDescription(business.getMessage());
        }
        return Status.INTERNAL.withDescription(CommonErrorCode.INTERNAL_ERROR.message());
    }

    /// Maps an HTTP status to the equivalent gRPC status code.
    public static Status.Code toStatusCode(int httpStatus) {
        return switch (httpStatus) {
            case 400, 415, 422 -> Status.Code.INVALID_ARGUMENT;
            case 401 -> Status.Code.UNAUTHENTICATED;
            case 403 -> Status.Code.PERMISSION_DENIED;
            case 404 -> Status.Code.NOT_FOUND;
            case 409 -> Status.Code.ABORTED;
            case 413, 429 -> Status.Code.RESOURCE_EXHAUSTED;
            case 500 -> Status.Code.INTERNAL;
            case 502, 503, 504 -> Status.Code.UNAVAILABLE;
            default -> Status.Code.UNKNOWN;
        };
    }

    /// Fallback mapping used when the trailer does not carry the original error code.
    public static ErrorCode toErrorCode(Status.Code statusCode) {
        return switch (statusCode) {
            case INVALID_ARGUMENT, OUT_OF_RANGE, FAILED_PRECONDITION -> CommonErrorCode.BAD_REQUEST;
            case UNAUTHENTICATED -> CommonErrorCode.UNAUTHORIZED;
            case PERMISSION_DENIED -> CommonErrorCode.FORBIDDEN;
            case NOT_FOUND -> CommonErrorCode.NOT_FOUND;
            case ABORTED, ALREADY_EXISTS -> CommonErrorCode.CONFLICT;
            case RESOURCE_EXHAUSTED -> CommonErrorCode.TOO_MANY_REQUESTS;
            case UNAVAILABLE -> CommonErrorCode.SERVICE_UNAVAILABLE;
            default -> CommonErrorCode.INTERNAL_ERROR;
        };
    }

    /// Reads the business error code written by the server.
    public static Optional<String> errorCode(Metadata trailers) {
        if (trailers == null) {
            return Optional.empty();
        }
        String value = trailers.get(ERROR_CODE_KEY);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    /// Reads the business error code from a failed call.
    public static Optional<String> errorCode(StatusRuntimeException exception) {
        return errorCode(exception.getTrailers());
    }

    /// Restores a [BusinessException] from a failed call.
    ///
    /// The original code is taken from the trailer when present; otherwise the gRPC
    /// status is mapped to the closest [CommonErrorCode].
    public static BusinessException toBusinessException(StatusRuntimeException exception) {
        String description = exception.getStatus().getDescription();
        ErrorCode fallback = toErrorCode(exception.getStatus().getCode());
        String message = description != null && !description.isBlank() ? description : fallback.message();
        return errorCode(exception)
                .map(code -> new BusinessException(new TransportErrorCode(code, message, fallback.httpStatus()), message))
                .orElseGet(() -> new BusinessException(fallback, message));
    }

    /// Error descriptor rebuilt on the client from a gRPC failure.
    ///
    /// @param code       original stable business code
    /// @param message    description sent by the server
    /// @param httpStatus HTTP status implied by the gRPC status code
    public record TransportErrorCode(String code, String message, int httpStatus) implements ErrorCode {
    }
}
