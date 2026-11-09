package io.github.foundry4j.forge.core.error;

/// Error codes shipped with the framework.
///
/// Application modules are expected to declare their own enums implementing
/// [ErrorCode] when they need domain specific codes.
public enum CommonErrorCode implements ErrorCode {

    /// The request could not be parsed or failed bean validation.
    BAD_REQUEST("COMMON_400", "Bad request", 400),

    /// The requested API version is not supported by the endpoint.
    API_VERSION_UNSUPPORTED("COMMON_400_API_VERSION", "Unsupported API version", 400),

    /// Authentication is required.
    UNAUTHORIZED("COMMON_401", "Unauthorized", 401),

    /// The caller is not allowed to perform the operation.
    FORBIDDEN("COMMON_403", "Forbidden", 403),

    /// The addressed resource does not exist.
    NOT_FOUND("COMMON_404", "Resource not found", 404),

    /// The HTTP method is not supported by the resource.
    METHOD_NOT_ALLOWED("COMMON_405", "Method not allowed", 405),

    /// The request conflicts with the current state, including duplicated idempotent keys.
    CONFLICT("COMMON_409", "Request conflict", 409),

    /// The resource is temporarily locked by another caller.
    LOCKED("COMMON_423", "Resource is locked", 423),

    /// The uploaded payload exceeds the configured size limit.
    PAYLOAD_TOO_LARGE("COMMON_413", "Payload too large", 413),

    /// The uploaded media type is not accepted by the endpoint.
    UNSUPPORTED_MEDIA_TYPE("COMMON_415", "Unsupported media type", 415),

    /// Too many requests were issued inside the configured window.
    TOO_MANY_REQUESTS("COMMON_429", "Too many requests", 429),

    /// An unexpected server side failure happened.
    INTERNAL_ERROR("COMMON_500", "Internal server error", 500),

    /// A downstream dependency is unavailable.
    SERVICE_UNAVAILABLE("COMMON_503", "Service unavailable", 503),

    /// The circuit breaker is open and rejected the call.
    CIRCUIT_OPEN("COMMON_504", "Circuit breaker is open", 503);

    private final String code;
    private final String message;
    private final int httpStatus;

    CommonErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }
}
