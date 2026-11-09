package io.github.foundry4j.forge.notification;

import io.github.foundry4j.forge.core.result.PageResult;

/// Inbox operations of the in-application notification channel.
///
/// The REST controller of the module is a thin layer on top of these methods, and an
/// application can call them directly when it needs a custom endpoint.
public interface NotificationInbox {

    /// Notifications of one recipient, newest first.
    PageResult<NotificationMessage> listFor(String userId, boolean unreadOnly, int page, int size);

    /// Number of unread notifications of a recipient.
    long unreadCount(String userId);

    /// Marks one notification as read.
    void markRead(String userId, long notificationId);

    /// Marks every unread notification of a recipient as read.
    ///
    /// @return number of notifications updated
    int markAllRead(String userId);

    /// Deletes a notification of a recipient.
    void delete(String userId, long notificationId);
}
