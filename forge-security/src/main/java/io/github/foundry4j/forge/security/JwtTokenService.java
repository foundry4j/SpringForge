package io.github.foundry4j.forge.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/// Issues and verifies HS256 JSON Web Tokens.
///
/// The implementation is dependency free: only the JDK MAC and Base64 support are
/// used, and the payload is serialized with the application `ObjectMapper`. A token
/// looks like the well known `header.payload.signature` triple.
///
/// Verification checks the signature, the issuer and the expiration timestamp. Any
/// malformed or expired token is reported as empty, so a caller can decide whether
/// to answer `401`.
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEADER = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final byte[] secret;
    private final String issuer;
    private final long ttlSeconds;
    private final ObjectMapper objectMapper;

    /// Creates the service from the JWT properties.
    public JwtTokenService(JwtProperties properties, ObjectMapper objectMapper) {
        if (properties.secret() == null || properties.secret().length() < 16) {
            throw new IllegalArgumentException("forge.jwt.secret must contain at least 16 characters");
        }
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.issuer = properties.issuer();
        this.ttlSeconds = properties.ttl().toSeconds();
        this.objectMapper = objectMapper == null ? JsonMapper.builder().build() : objectMapper;
    }

    /// Issues a token for a principal.
    public String issue(LoginUser user) {
        Instant now = Instant.now();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", user.userId());
        claims.put("name", user.username());
        claims.put("roles", user.roles());
        claims.put("perms", user.permissions());
        claims.putAll(user.claims());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(ttlSeconds).getEpochSecond());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(claims));
        String signingInput = HEADER + "." + payload;
        return signingInput + "." + sign(signingInput);
    }

    /// Verifies a token and rebuilds the principal.
    public Optional<LoginUser> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        String signingInput = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII))) {
            return Optional.empty();
        }
        try {
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!issuer.equals(claims.path("iss").asString())) {
                return Optional.empty();
            }
            long exp = claims.path("exp").asLong();
            if (exp > 0 && Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }
            String userId = claims.path("sub").asString();
            if (userId == null || userId.isBlank()) {
                return Optional.empty();
            }
            String username = claims.path("name").asString(userId);
            return Optional.of(new LoginUser(userId, username,
                    readStrings(claims.path("roles")), readStrings(claims.path("perms")), Map.of()));
        } catch (RuntimeException _) {
            return Optional.empty();
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 is not available", ex);
        }
    }

    private Set<String> readStrings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        node.values().forEach(value -> {
            String text = value.asString();
            if (text != null && !text.isBlank()) {
                values.add(text);
            }
        });
        return values;
    }
}
