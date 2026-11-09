package io.github.foundry4j.forge.crypto.autoconfigure;

import io.github.foundry4j.forge.core.autoconfigure.ForgeCoreAutoConfiguration;
import io.github.foundry4j.forge.crypto.AesGcmCryptoService;
import io.github.foundry4j.forge.crypto.CryptoProperties;
import io.github.foundry4j.forge.crypto.CryptoService;
import io.github.foundry4j.forge.crypto.CryptoServiceHolder;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Wires the AES-256-GCM service when `forge.crypto.key` is configured.
@AutoConfiguration(after = ForgeCoreAutoConfiguration.class)
@ConditionalOnProperty(prefix = "forge.crypto", name = "key")
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoAutoConfiguration {

    /// Default crypto service.
    @Bean
    @ConditionalOnMissingBean
    public CryptoService forgeCryptoService(CryptoProperties properties) {
        return new AesGcmCryptoService(properties.key());
    }

    /// Publishes the service to the JPA attribute converter.
    @Bean
    public SmartInitializingSingleton forgeCryptoServiceBinding(CryptoService service) {
        return () -> CryptoServiceHolder.bind(service);
    }
}
