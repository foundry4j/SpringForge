package io.github.foundry4j.forge.security.filter;

import io.github.foundry4j.forge.security.AuthContextHolder;
import io.github.foundry4j.forge.security.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// Authenticates requests from the `Authorization: Bearer <jwt>` header.
///
/// A valid token binds the principal to the current thread for the duration of the
/// request. An invalid token is ignored, so the endpoint decides whether anonymous
/// access is acceptable; protected endpoints answer `401` through the RBAC aspect.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtTokenService tokenService;

    /// Creates the filter.
    public JwtAuthenticationFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            tokenService.verify(header.substring(BEARER.length()).trim())
                    .ifPresent(AuthContextHolder::set);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthContextHolder.clear();
        }
    }
}
