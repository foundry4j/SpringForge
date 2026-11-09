package io.github.foundry4j.forge.web.filter;

import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.web.ForgeWebProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// Writes one access log line per request.
///
/// The trace identifier is taken from [ForgeContext], so it matches the value of the
/// `X-Trace-Id` response header and the identifier used by application logs.
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AccessLogFilter.class);

    private final ForgeWebProperties properties;

    /// Creates the filter.
    public AccessLogFilter(ForgeWebProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.accessLog().enabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long costMillis = (System.nanoTime() - start) / 1_000_000;
            LOG.info("{} {} -> {} ({} ms) traceId={}", request.getMethod(),
                    request.getRequestURI(), response.getStatus(), costMillis, ForgeContext.traceId());
        }
    }
}
