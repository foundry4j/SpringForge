package io.github.foundry4j.forge.audit;

import io.github.foundry4j.forge.core.context.ForgeContext;

/// Default actor provider, backed by the request context.
public class DefaultAuditActorProvider implements AuditActorProvider {

    @Override
    public String currentActor() {
        return ForgeContext.userId().orElse("system");
    }
}
