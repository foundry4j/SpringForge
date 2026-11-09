package io.github.foundry4j.forge.notification.sender;

import io.github.foundry4j.forge.notification.NotificationChannel;
import io.github.foundry4j.forge.notification.NotificationMessage;
import io.github.foundry4j.forge.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Default [NotificationSender] of the [NotificationChannel#SMS] channel.
///
/// It writes the message to the log, which keeps local development free of an SMS gateway.
/// Declare an SMS capable `NotificationSender` bean to replace it.
public class SmsNotificationSender implements NotificationSender {

    private static final Logger LOG = LoggerFactory.getLogger(SmsNotificationSender.class);

    /// {@inheritDoc}
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    /// {@inheritDoc}
    @Override
    public void send(NotificationMessage message) {
        LOG.info("Sending SMS [{}] to [{}]", message.getId(), message.getRecipient());
    }
}
