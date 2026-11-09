package io.github.foundry4j.forge.notification;

/// Resolves the user identifier of the current REST request.
///
/// The default implementation reads
/// [io.github.foundry4j.forge.core.context.ForgeContext]. Applications that authenticate
/// with the security module declare a bean backed by
/// `io.github.foundry4j.forge.security.AuthContext`:
///
/// ```java
/// @Bean
/// NotificationUserProvider notificationUserProvider(AuthContext authContext) {
///     return () -> authContext.requireUser().userId();
/// }
/// ```
@FunctionalInterface
public interface NotificationUserProvider {

    /// Identifier of the caller.
    String currentUserId();
}
