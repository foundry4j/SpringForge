package io.github.foundry4j.forge.notification.sender;

import io.github.foundry4j.forge.notification.NotificationChannel;
import io.github.foundry4j.forge.notification.NotificationMessage;
import io.github.foundry4j.forge.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Default [NotificationSender] of the [NotificationChannel#SYSTEM] channel.
///
/// An in-application notification is already delivered by persisting it, so this sender
/// only confirms the delivery in the log. Applications usually read the notifications of a
/// user from [io.github.foundry4j.forge.notification.NotificationRepository].
public class SystemNotificationSender implements NotificationSender {

    private static final Logger LOG = LoggerFactory.getLogger(SystemNotificationSender.class);

    /// {@inheritDoc}
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SYSTEM;
    }

    /// {@inheritDoc}
    @Override
    public void send(NotificationMessage message) {
        LOG.info("Stored system notification [{}] for recipient [{}]", message.getId(), message.getRecipient());
    }
}
