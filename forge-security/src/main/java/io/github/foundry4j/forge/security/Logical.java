package io.github.foundry4j.forge.security;

/// Combination rule of multiple role or permission requirements.
public enum Logical {

    /// At least one of the declared values is required.
    ANY,

    /// Every declared value is required.
    ALL
}
