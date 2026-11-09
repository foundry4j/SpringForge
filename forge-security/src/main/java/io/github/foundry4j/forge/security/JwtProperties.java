package io.github.foundry4j.forge.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Configuration of JWT issuing and validation.
///
/// @param secret HMAC secret, at least 32 characters in production
/// @param issuer expected `iss` claim
/// @param ttl    lifetime of newly issued tokens
@ConfigurationProperties("forge.jwt")
public record JwtProperties(String secret, String issuer, Duration ttl) {

    /// Applies the defaults `forge` and one hour.
    public JwtProperties {
        issuer = issuer == null || issuer.isBlank() ? "forge" : issuer;
        ttl = ttl == null || ttl.isZero() || ttl.isNegative() ? Duration.ofHours(1) : ttl;
    }
}
