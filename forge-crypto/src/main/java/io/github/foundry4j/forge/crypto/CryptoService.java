package io.github.foundry4j.forge.crypto;

/// Symmetric encryption service used for columns and message fields.
public interface CryptoService {

    /// Encrypts a value; the result is a self contained, Base64 encoded token.
    String encrypt(String plainText);

    /// Decrypts a token produced by [encrypt].
    ///
    /// @throws IllegalStateException when the token was tampered with
    String decrypt(String cipherText);
}
