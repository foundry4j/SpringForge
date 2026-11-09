package io.github.foundry4j.forge.crypto;

/// Static bridge used by the JPA attribute converter, which is instantiated by
/// Hibernate rather than by Spring.
public final class CryptoServiceHolder {

    private static volatile CryptoService service;

    private CryptoServiceHolder() {
    }

    /// Binds the configured service.
    public static void bind(CryptoService cryptoService) {
        service = cryptoService;
    }

    /// Returns the configured service.
    static CryptoService service() {
        CryptoService current = service;
        if (current == null) {
            throw new IllegalStateException(
                    "Encrypted columns require the crypto module and a configured forge.crypto.key");
        }
        return current;
    }
}
