package io.github.foundry4j.forge.notification.sender;

import io.github.foundry4j.forge.notification.NotificationChannel;
import io.github.foundry4j.forge.notification.NotificationMessage;
import io.github.foundry4j.forge.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Default [NotificationSender] of the [NotificationChannel#EMAIL] channel.
///
/// It writes the message to the log, which keeps local development free of an SMTP server.
/// Declare an email capable `NotificationSender` bean to replace it.
public class EmailNotificationSender implements NotificationSender {

    private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationSender.class);

    /// {@inheritDoc}
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    /// {@inheritDoc}
    @Override
    public void send(NotificationMessage message) {
        LOG.info("Sending e-mail [{}] to [{}]: {}", message.getId(), message.getRecipient(), message.getSubject());
    }
}
