package io.github.foundry4j.forge.notification;

/// Business type of a notification.
///
/// The type is stored as a string, so an application can either use one of the
/// provided values or add its own constants. It drives filters and icons in the user
/// interface; it does not influence delivery.
public enum NotificationType {

    /// Announcement, maintenance notice or other system message.
    SYSTEM,

    /// A task was assigned to the recipient.
    TASK,

    /// Someone replied to the recipient.
    COMMENT,

    /// Someone liked content of the recipient.
    LIKE,

    /// Someone mentioned the recipient.
    MENTION,

    /// An approval request changed state.
    APPROVAL,

    /// Application defined business type.
    CUSTOM
}
