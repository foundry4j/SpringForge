package io.github.foundry4j.forge.security;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;

import java.util.Optional;

/// Query API for the authenticated principal.
///
/// ```java
/// LoginUser user = authContext.requireUser();
/// if (!authContext.hasPermission("order:refund")) { ... }
/// ```
public class AuthContext {

    /// Returns the current principal, if any.
    public Optional<LoginUser> user() {
        return AuthContextHolder.current();
    }

    /// Returns the current principal or throws a `401` business exception.
    public LoginUser requireUser() {
        return user().orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED,
                "Authentication is required"));
    }

    /// Whether a principal is bound to the current thread.
    public boolean authenticated() {
        return user().isPresent();
    }

    /// Identifier of the current principal, or `anonymous`.
    public String userId() {
        return user().map(LoginUser::userId).orElse("anonymous");
    }

    /// Whether the current principal carries a role.
    public boolean hasRole(String role) {
        return user().map(user -> user.hasRole(role)).orElse(false);
    }

    /// Whether the current principal carries a permission.
    public boolean hasPermission(String permission) {
        return user().map(user -> user.hasPermission(permission)).orElse(false);
    }
}
