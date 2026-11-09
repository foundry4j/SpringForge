package io.github.foundry4j.forge.ratelimit;

/// Dimension used to isolate rate limit quotas.
public enum KeyStrategy {

    /// Per authenticated user, falling back to the client address for anonymous traffic.
    USER,

    /// Per client address.
    IP,

    /// One bucket shared by every caller.
    GLOBAL
}
