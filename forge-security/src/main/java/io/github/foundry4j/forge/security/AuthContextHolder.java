package io.github.foundry4j.forge.security;

import java.util.Optional;

/// Thread bound holder of the authenticated principal.
///
/// The servlet filter fills it from the bearer token, and background code can use
/// [runAs] to give a job an identity:
///
/// ```java
/// AuthContextHolder.runAs(LoginUser.system(), () -> job.execute());
/// ```
///
/// Always remove the value when a thread is reused; [runAs] does this automatically.
public final class AuthContextHolder {

    private static final ThreadLocal<LoginUser> CURRENT = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    /// Returns the current principal, if the thread is authenticated.
    public static Optional<LoginUser> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /// Binds a principal to the current thread; `null` clears it.
    public static void set(LoginUser user) {
        if (user == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(user);
        }
    }

    /// Clears the current thread.
    public static void clear() {
        CURRENT.remove();
    }

    /// Runs an action with a temporary principal and restores the previous one.
    public static void runAs(LoginUser user, Runnable action) {
        LoginUser previous = CURRENT.get();
        set(user);
        try {
            action.run();
        } finally {
            set(previous);
        }
    }
}
