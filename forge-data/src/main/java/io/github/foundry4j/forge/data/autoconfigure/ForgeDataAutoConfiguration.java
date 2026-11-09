package io.github.foundry4j.forge.data.autoconfigure;

import io.github.foundry4j.forge.core.autoconfigure.ForgeCoreAutoConfiguration;
import io.github.foundry4j.forge.core.context.ForgeContextProperties;
import io.github.foundry4j.forge.data.ForgeDataModule;
import io.github.foundry4j.forge.data.audit.ForgeJpaAuditingConfiguration;
import io.github.foundry4j.forge.data.dict.DictItemRepository;
import io.github.foundry4j.forge.data.dict.DictService;
import io.github.foundry4j.forge.data.dict.DictTypeRepository;
import io.github.foundry4j.forge.data.tenant.ForgeTenantIdentifierResolver;
import jakarta.persistence.EntityManager;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/// Wires the JPA base layer: auditing, multi-tenancy, dictionary service and scanning.
@AutoConfiguration(after = ForgeCoreAutoConfiguration.class)
@ConditionalOnClass(EntityManager.class)
@EntityScan(basePackageClasses = ForgeDataModule.class)
@EnableJpaRepositories(basePackageClasses = ForgeDataModule.class)
@Import(ForgeJpaAuditingConfiguration.class)
public class ForgeDataAutoConfiguration {

    /// Resolves the tenant for Hibernate's discriminator based multi-tenancy.
    @Bean
    @ConditionalOnMissingBean
    public CurrentTenantIdentifierResolver<String> forgeTenantIdentifierResolver(ForgeContextProperties properties) {
        return new ForgeTenantIdentifierResolver(properties.defaultTenant());
    }

    /// Registers the resolver with Hibernate.
    @Bean
    public HibernatePropertiesCustomizer forgeTenantHibernateCustomizer(
            CurrentTenantIdentifierResolver<String> resolver) {
        return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }

    /// Dictionary lookup and caching service.
    @Bean
    @ConditionalOnMissingBean
    public DictService forgeDictService(DictTypeRepository typeRepository, DictItemRepository itemRepository) {
        return new DictService(typeRepository, itemRepository);
    }
}
