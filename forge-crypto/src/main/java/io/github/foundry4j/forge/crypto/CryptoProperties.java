package io.github.foundry4j.forge.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the crypto module.
///
/// @param key secret used to derive the AES-256 key; at least 16 characters
@ConfigurationProperties("forge.crypto")
public record CryptoProperties(String key) {
}
