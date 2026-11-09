package io.github.foundry4j.forge.notification.web;

import io.github.foundry4j.forge.core.result.PageResult;
import io.github.foundry4j.forge.notification.NotificationService;
import io.github.foundry4j.forge.notification.NotificationUserProvider;
import io.github.foundry4j.forge.notification.NotificationView;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// Inbox endpoints of the in-application notification channel.
///
/// The caller is resolved by [NotificationUserProvider]; the default implementation
/// reads the request context. Every operation is scoped to the caller, so one user
/// can never read or delete the notifications of another user.
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationUserProvider userProvider;

    /// Creates the controller.
    public NotificationController(NotificationService service, NotificationUserProvider userProvider) {
        this.service = service;
        this.userProvider = userProvider;
    }

    /// Lists the inbox of the caller.
    @GetMapping
    public PageResult<NotificationView> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.listFor(userProvider.currentUserId(), unreadOnly, page, size)
                .map(NotificationView::from);
    }

    /// Returns the number of unread notifications of the caller.
    @GetMapping("/unread-count")
    public UnreadCount unreadCount() {
        return new UnreadCount(service.unreadCount(userProvider.currentUserId()));
    }

    /// Marks one notification of the caller as read.
    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable long id) {
        service.markRead(userProvider.currentUserId(), id);
    }

    /// Marks every unread notification of the caller as read.
    @PatchMapping("/read-all")
    public ReadAllResult markAllRead() {
        return new ReadAllResult(service.markAllRead(userProvider.currentUserId()));
    }

    /// Deletes one notification of the caller.
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        service.delete(userProvider.currentUserId(), id);
    }

    /// Number of unread notifications.
    ///
    /// @param count unread count
    public record UnreadCount(long count) {
    }

    /// Result of a mark all read operation.
    ///
    /// @param updated number of notifications that changed state
    public record ReadAllResult(int updated) {
    }
}
