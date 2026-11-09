package io.github.foundry4j.forge.web.filter;

import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.core.context.ForgeContextProperties;
import io.github.foundry4j.forge.core.support.Ids;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// Binds tenant, user and trace identifiers for the duration of a request.
///
/// The filter uses [ForgeContext#carrier] so the [java.lang.ScopedValue] binding is
/// visible to every downstream call, including virtual threads created inside the
/// request.
public class ForgeRequestContextFilter extends OncePerRequestFilter {

    private static final String TRACE_MDC_KEY = "traceId";
    private static final String TENANT_MDC_KEY = "tenantId";

    private final ForgeContextProperties properties;

    /// Creates the filter.
    public ForgeRequestContextFilter(ForgeContextProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(properties.traceHeader());
        if (traceId == null || traceId.isBlank()) {
            traceId = Ids.traceId();
        }
        String tenantId = request.getHeader(properties.tenantHeader());
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = properties.defaultTenant();
        }
        String userId = request.getHeader(properties.userHeader());
        var caller = new ForgeContext.Caller(tenantId, userId, traceId);

        response.setHeader(properties.traceHeader(), traceId);
        MDC.put(TRACE_MDC_KEY, traceId);
        MDC.put(TENANT_MDC_KEY, tenantId);
        try {
            ForgeContext.carrier(caller).call(() -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex);
        } finally {
            MDC.remove(TRACE_MDC_KEY);
            MDC.remove(TENANT_MDC_KEY);
        }
    }
}
