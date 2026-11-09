package io.github.foundry4j.forge.web.ratelimit;

import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.ratelimit.KeyStrategy;
import io.github.foundry4j.forge.ratelimit.RateLimitSubjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/// Resolves rate limit subjects from the request.
///
/// `USER` prefers the authenticated user bound to [ForgeContext] and falls back to the
/// client address, `IP` always uses the client address and `GLOBAL` shares one bucket.
/// `X-Forwarded-For` and `X-Real-IP` are honoured, so limits keep working behind a proxy.
public class WebRateLimitSubjectResolver implements RateLimitSubjectResolver {

    @Override
    public String resolve(KeyStrategy strategy, Method method, Object target, Object[] args) {
        return switch (strategy) {
            case GLOBAL -> "global";
            case USER -> ForgeContext.userId().orElseGet(WebRateLimitSubjectResolver::clientAddress);
            case IP -> clientAddress();
        };
    }

    private static String clientAddress() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "local";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
