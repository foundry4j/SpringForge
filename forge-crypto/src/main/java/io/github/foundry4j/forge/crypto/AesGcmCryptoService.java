package io.github.foundry4j.forge.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/// AES-256-GCM implementation of [CryptoService].
///
/// Every value gets a fresh 96 bit initialization vector, and the 128 bit GCM tag
/// makes tampering detectable. The token layout is `Base64(iv || ciphertext || tag)`,
/// so it can be stored in a normal `varchar` column.
///
/// The configured secret is hashed with SHA-256 to obtain the 32 byte key, which
/// accepts secrets of any length without silently truncating them.
public class AesGcmCryptoService implements CryptoService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    /// Creates the service from a non blank secret.
    public AesGcmCryptoService(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("forge.crypto.key must not be blank");
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] token = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, token, 0, iv.length);
            System.arraycopy(encrypted, 0, token, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(token);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to encrypt value", ex);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] token = Base64.getDecoder().decode(cipherText);
            if (token.length <= IV_LENGTH) {
                throw new IllegalStateException("Cipher text is too short");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(token, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] decrypted = cipher.doFinal(token, IV_LENGTH, token.length - IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Failed to decrypt value", ex);
        }
    }
}
