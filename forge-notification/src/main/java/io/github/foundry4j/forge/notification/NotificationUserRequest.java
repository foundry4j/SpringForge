package io.github.foundry4j.forge.notification;

/// Request to notify a user of an application event.
///
/// The notification is stored in the inbox of the user, pushed to an open WebSocket
/// session and, when enabled, fanned out to the e-mail and SMS channels.
///
/// @param userId  recipient user identifier
/// @param type    business type
/// @param title   short title shown in the inbox
/// @param content message body
/// @param link    optional link to the business object
/// @param actorId user who triggered the event, may be `null`
public record NotificationUserRequest(String userId,
                                      NotificationType type,
                                      String title,
                                      String content,
                                      String link,
                                      String actorId) {

    /// Creates a request without link and actor.
    public static NotificationUserRequest of(String userId, NotificationType type,
                                             String title, String content) {
        return new NotificationUserRequest(userId, type, title, content, null, null);
    }
}
