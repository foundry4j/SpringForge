package io.github.foundry4j.forge.web.result;

import io.github.foundry4j.forge.core.result.ApiResult;
import io.github.foundry4j.forge.web.ForgeWebProperties;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/// Wraps successful controller return values into [ApiResult].
///
/// Error responses are produced by [io.github.foundry4j.forge.web.error.GlobalExceptionHandler]
/// and are never wrapped. Plain strings and byte arrays are skipped as well so that
/// endpoints returning files or text keep working.
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ForgeWebProperties properties;

    /// Creates the advice.
    public ApiResponseAdvice(ForgeWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return properties.responseWrapping();
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null || isAlreadyWrapped(body)) {
            return body;
        }
        return ApiResult.ok(body);
    }

    private boolean isAlreadyWrapped(Object body) {
        return body instanceof ApiResult<?>
                || body instanceof ProblemDetail
                || body instanceof CharSequence
                || body instanceof byte[];
    }
}
