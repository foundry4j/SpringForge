package io.github.foundry4j.forge.notification;

import java.util.Optional;

/// Resolves the contact data of a recipient.
///
/// The notification module does not know the user model of the application, so the
/// e-mail and SMS channels ask this SPI to translate a user identifier into an
/// address. Declare a bean to look the values up in the user table.
///
/// The default implementation resolves nothing, which makes both channels skip
/// notifications that are addressed with a bare user identifier.
public interface NotificationContactResolver {

    /// E-mail address of the user.
    default Optional<String> emailOf(String userId) {
        return Optional.empty();
    }

    /// Mobile number of the user.
    default Optional<String> phoneOf(String userId) {
        return Optional.empty();
    }
}
