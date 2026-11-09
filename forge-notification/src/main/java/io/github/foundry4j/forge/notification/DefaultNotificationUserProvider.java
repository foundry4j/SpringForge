package io.github.foundry4j.forge.notification;

import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;

/// Default [NotificationUserProvider], backed by the request context.
public class DefaultNotificationUserProvider implements NotificationUserProvider {

    @Override
    public String currentUserId() {
        return ForgeContext.userId().orElseThrow(() -> new BusinessException(
                CommonErrorCode.UNAUTHORIZED, "Authentication is required"));
    }
}
