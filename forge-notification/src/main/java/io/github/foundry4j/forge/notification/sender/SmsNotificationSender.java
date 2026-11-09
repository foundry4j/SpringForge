package io.github.foundry4j.forge.notification.sender;

import io.github.foundry4j.forge.notification.NotificationChannel;
import io.github.foundry4j.forge.notification.NotificationMessage;
import io.github.foundry4j.forge.notification.NotificationSender;
import io.github.foundry4j.forge.notification.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Default [NotificationSender] of the [NotificationChannel#SMS] channel.
///
/// When a [SmsSender] bean is present the message is handed to it; otherwise the
/// sender logs the message so that the module works without an SMS gateway.
public class SmsNotificationSender implements NotificationSender {

    private static final Logger LOG = LoggerFactory.getLogger(SmsNotificationSender.class);

    private final SmsSender delegate;

    /// Creates a logging sender.
    public SmsNotificationSender() {
        this(null);
    }

    /// Creates a sender backed by a provider specific implementation.
    public SmsNotificationSender(SmsSender delegate) {
        this.delegate = delegate;
    }

    /// {@inheritDoc}
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    /// {@inheritDoc}
    @Override
    public void send(NotificationMessage message) {
        if (delegate != null) {
            delegate.send(message.getRecipient(), message.getContent());
            return;
        }
        LOG.info("SMS to [{}]: {}", message.getRecipient(), message.getContent());
    }
}
