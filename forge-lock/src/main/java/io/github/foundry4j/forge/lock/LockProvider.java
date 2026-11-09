package io.github.foundry4j.forge.lock;

/// Supported lock backends.
public enum LockProvider {

    /// JVM local reentrant locks. Useful for development and single node setups.
    LOCAL,

    /// Redis `SET NX PX` locks. Required for clustered deployments.
    REDIS
}
