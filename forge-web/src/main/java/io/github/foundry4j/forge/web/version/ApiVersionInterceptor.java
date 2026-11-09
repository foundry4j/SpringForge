package io.github.foundry4j.forge.web.version;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import io.github.foundry4j.forge.web.ForgeWebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/// Negotiates the API version of every request.
///
/// The requested version is read from the configured header, then from a `vN` first
/// path segment and finally from the configured default. Handlers annotated with
/// [ApiVersion] reject versions outside their supported list and deprecated handlers
/// advertise their deprecation through response headers.
public class ApiVersionInterceptor implements HandlerInterceptor {

    /// Header carrying the negotiated version in the response.
    public static final String VERSION_HEADER = "X-API-Version";

    /// Header marking a deprecated endpoint.
    public static final String DEPRECATION_HEADER = "Deprecation";

    /// Header carrying the planned removal date.
    public static final String SUNSET_HEADER = "Sunset";

    private static final String REQUESTED_VERSION = ApiVersionInterceptor.class.getName() + ".requested";

    private final ForgeWebProperties properties;

    /// Creates the interceptor.
    public ApiVersionInterceptor(ForgeWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ForgeWebProperties.ApiVersion settings = properties.apiVersion();
        if (!settings.enabled()) {
            return true;
        }
        String requested = normalize(resolve(request, settings));
        request.setAttribute(REQUESTED_VERSION, requested);
        response.setHeader(settings.header(), requested);

        ApiVersion annotation = findAnnotation(handler);
        if (annotation == null) {
            return true;
        }
        List<String> supported = Arrays.stream(annotation.value())
                .map(ApiVersionInterceptor::normalize)
                .toList();
        if (!supported.contains(requested)) {
            throw new BusinessException(CommonErrorCode.API_VERSION_UNSUPPORTED,
                    "Unsupported API version [%s], supported versions: %s"
                            .formatted(requested, String.join(", ", supported)));
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        ForgeWebProperties.ApiVersion settings = properties.apiVersion();
        if (!settings.enabled() || !(handler instanceof HandlerMethod method)) {
            return;
        }
        ApiVersion annotation = findAnnotation(handler);
        if (annotation == null || !isDeprecated(method, annotation)) {
            return;
        }
        response.setHeader(DEPRECATION_HEADER, "true");
        if (!annotation.sunset().isBlank()) {
            response.setHeader(SUNSET_HEADER, annotation.sunset());
        }
        if (!annotation.successor().isBlank()) {
            response.setHeader("Link", "<%s>; rel=\"successor-version\"".formatted(annotation.successor()));
        }
    }

    /// Returns the version negotiated for the current request.
    public static String requestedVersion(HttpServletRequest request) {
        Object value = request.getAttribute(REQUESTED_VERSION);
        return value == null ? null : value.toString();
    }

    private String resolve(HttpServletRequest request, ForgeWebProperties.ApiVersion settings) {
        String header = request.getHeader(settings.header());
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String first = path.startsWith("/") ? path.substring(1) : path;
        int slash = first.indexOf('/');
        String segment = slash >= 0 ? first.substring(0, slash) : first;
        if (segment.matches("(?i)v?\\d+")) {
            return segment;
        }
        return settings.defaultVersion();
    }

    private boolean isDeprecated(HandlerMethod method, ApiVersion annotation) {
        return annotation.deprecated()
                || method.getMethod().isAnnotationPresent(Deprecated.class)
                || method.getBeanType().isAnnotationPresent(Deprecated.class);
    }

    private ApiVersion findAnnotation(Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return null;
        }
        ApiVersion onMethod = method.getMethodAnnotation(ApiVersion.class);
        return onMethod != null ? onMethod : method.getBeanType().getAnnotation(ApiVersion.class);
    }

    static String normalize(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String trimmed = version.trim().toLowerCase(Locale.ROOT);
        return trimmed.matches("\\d+") ? "v" + trimmed : trimmed;
    }
}
