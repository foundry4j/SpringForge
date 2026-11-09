package io.github.foundry4j.forge.web.idempotency;

import io.github.foundry4j.forge.idempotency.Idempotent;
import io.github.foundry4j.forge.idempotency.IdempotentKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/// Uses the `Idempotency-Key` header and delegates to the expression resolver otherwise.
///
/// A client supplied key takes precedence over the annotation expression only when the
/// annotation does not declare one. Without any key the delegate derives the key from the
/// invocation arguments.
public class HttpIdempotentKeyResolver implements IdempotentKeyResolver {

    /// Header carrying client supplied idempotency keys.
    public static final String HEADER = "Idempotency-Key";

    private final IdempotentKeyResolver delegate;

    /// Creates the resolver.
    public HttpIdempotentKeyResolver(IdempotentKeyResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public String resolve(Idempotent config, Method method, Object target, Object[] args) {
        if (config.key().isBlank()) {
            HttpServletRequest request = currentRequest();
            String header = request == null ? null : request.getHeader(HEADER);
            if (header != null && !header.isBlank()) {
                return method.getDeclaringClass().getName() + "#" + method.getName() + ":" + header;
            }
        }
        return delegate.resolve(config, method, target, args);
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
