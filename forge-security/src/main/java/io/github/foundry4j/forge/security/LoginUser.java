package io.github.foundry4j.forge.security;

import java.util.Map;
import java.util.Set;

/// Authenticated principal of a request.
///
/// @param userId      stable user identifier, written to audit records
/// @param username    display name
/// @param roles       role codes, for example `ADMIN`
/// @param permissions fine grained permission codes, for example `order:read`
/// @param claims      additional token claims, never `null`
public record LoginUser(String userId,
                        String username,
                        Set<String> roles,
                        Set<String> permissions,
                        Map<String, Object> claims) {

    /// Copies the collections so that an authenticated principal is immutable.
    public LoginUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        claims = claims == null ? Map.of() : Map.copyOf(claims);
    }

    /// Creates a principal without extra claims.
    public LoginUser(String userId, String username, Set<String> roles, Set<String> permissions) {
        this(userId, username, roles, permissions, Map.of());
    }

    /// Principal used by background jobs and system initiated changes.
    public static LoginUser system() {
        return new LoginUser("system", "system", Set.of("SYSTEM"), Set.of(), Map.of());
    }

    /// Whether the principal carries a role.
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /// Whether the principal carries a permission.
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
