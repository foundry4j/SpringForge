package io.github.foundry4j.forge.test;

import io.github.foundry4j.forge.security.AuthContextHolder;
import io.github.foundry4j.forge.security.LoginUser;

import java.util.List;
import java.util.Map;
import java.util.Set;

/// Binds a fake authenticated principal to the current test thread.
///
/// ```java
/// @Test
/// void adminCanCloseOrder() {
///     TestAuth.authenticate(Set.of("ADMIN"));
///     // ...
/// }
/// ```
///
/// The helper writes to [AuthContextHolder], so permission checks through
/// `@RequiresRole` / `@RequiresPermission` see the simulated identity.
public final class TestAuth {

    private TestAuth() {
    }

    /// Signs in a test user with the given roles and no extra permissions.
    public static LoginUser authenticate(Set<String> roles) {
        return authenticate("test-user", "tester", roles, Set.of());
    }

    /// Signs in a test user with a complete identity.
    public static LoginUser authenticate(String userId, String username,
                                         Set<String> roles, Set<String> permissions) {
        LoginUser user = new LoginUser(userId, username, roles, permissions, Map.of());
        AuthContextHolder.set(user);
        return user;
    }

    /// Signs in a test user carrying the given permissions.
    public static LoginUser authenticateWithPermissions(Set<String> permissions) {
        return authenticate("test-user", "tester", Set.of(), permissions);
    }

    /// Removes the simulated identity.
    public static void anonymous() {
        AuthContextHolder.clear();
    }

    /// Returns the roles of the current identity as a list, handy for assertions.
    public static List<String> currentRoles() {
        return AuthContextHolder.current().map(user -> List.copyOf(user.roles())).orElseGet(() -> List.of());
    }
}
