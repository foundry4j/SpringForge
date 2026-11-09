package io.github.foundry4j.forge.audit;

/// Resolves the actor written to audit and operation logs.
///
/// The default implementation reads
/// [io.github.foundry4j.forge.core.context.ForgeContext]. Declare a bean to take the
/// identity from another source, for example from
/// `io.github.foundry4j.forge.security.AuthContext`.
@FunctionalInterface
public interface AuditActorProvider {

    /// Identifier of the current actor.
    String currentActor();
}
