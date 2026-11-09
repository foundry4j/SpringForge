package io.github.foundry4j.forge.web.error;

import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.error.ErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import io.github.foundry4j.forge.core.exception.TooManyRequestsException;
import io.github.foundry4j.forge.web.ForgeWebProperties;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Translates every exception into a RFC 9457 problem detail.
///
/// The problem carries the stable business `code`, the `traceId` and, for validation
/// failures, a `violations` array so that clients can highlight the offending fields.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VIOLATIONS = "violations";
    private static final String TRACE_ID = "traceId";
    private static final String CODE = "code";

    private final ForgeWebProperties properties;

    /// Creates the handler.
    public GlobalExceptionHandler(ForgeWebProperties properties) {
        this.properties = properties;
    }

    /// Handles all exceptions that carry an [ErrorCode].
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException exception) {
        ProblemDetail detail = problem(exception.errorCode(), exception.getMessage());
        if (exception instanceof TooManyRequestsException tooMany) {
            detail.setProperty("retryAfter", tooMany.retryAfterSeconds());
        }
        return detail;
    }

    /// Handles bean validation failures of `@RequestBody` arguments.
    @ExceptionHandler(BindException.class)
    public ProblemDetail handleBinding(BindException exception) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::violation)
                .toList();
        return validationProblem(violations);
    }

    /// Handles method level validation, for example constraints on `@RequestParam`.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(HandlerMethodValidationException exception) {
        List<FieldViolation> violations = new ArrayList<>();
        for (var result : exception.getParameterValidationResults()) {
            String parameter = result.getMethodParameter().getParameterName();
            for (var error : result.getResolvableErrors()) {
                violations.add(new FieldViolation(parameter, error.getDefaultMessage(), result.getArgument()));
            }
        }
        for (var error : exception.getCrossParameterValidationResults()) {
            violations.add(new FieldViolation("cross-parameter", error.getDefaultMessage(), null));
        }
        return validationProblem(violations);
    }

    /// Handles constraint violations raised by services annotated with `@Validated`.
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()))
                .toList();
        return validationProblem(violations);
    }

    /// Handles unreadable JSON bodies.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(CommonErrorCode.BAD_REQUEST, "Malformed request body");
    }

    /// Handles missing request parameters.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
        return problem(CommonErrorCode.BAD_REQUEST,
                "Required parameter [%s] is missing".formatted(exception.getParameterName()));
    }

    /// Handles parameters that cannot be converted to the declared type.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(CommonErrorCode.BAD_REQUEST,
                "Parameter [%s] has an invalid value".formatted(exception.getName()));
    }

    /// Handles unsupported HTTP methods.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return problem(CommonErrorCode.METHOD_NOT_ALLOWED, exception.getMessage());
    }

    /// Handles requests for unknown resources.
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException exception) {
        return problem(CommonErrorCode.NOT_FOUND, "No handler for [%s]".formatted(exception.getResourcePath()));
    }

    /// Handles exceptions that already carry an HTTP status.
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                Objects.requireNonNullElse(exception.getReason(), "Request failed"));
        detail.setTitle(HttpStatus.valueOf(exception.getStatusCode().value()).getReasonPhrase());
        detail.setProperty(CODE, "HTTP_" + exception.getStatusCode().value());
        detail.setProperty(TRACE_ID, ForgeContext.traceId());
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /// Catches everything else and hides implementation details from clients.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        LOG.error("Unhandled exception while serving request", exception);
        return problem(CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.message());
    }

    private ProblemDetail validationProblem(List<FieldViolation> violations) {
        ProblemDetail detail = problem(CommonErrorCode.BAD_REQUEST, "Request validation failed");
        detail.setProperty(VIOLATIONS, violations);
        return detail;
    }

    private ProblemDetail problem(ErrorCode errorCode, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(errorCode.httpStatus()),
                message == null || message.isBlank() ? errorCode.message() : message);
        detail.setTitle(errorCode.message());
        detail.setType(URI.create(properties.problemTypePrefix() + errorCode.code()));
        detail.setProperty(CODE, errorCode.code());
        detail.setProperty(TRACE_ID, ForgeContext.traceId());
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    private FieldViolation violation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }
}
