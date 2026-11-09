package io.github.foundry4j.forge.audit.autoconfigure;

import io.github.foundry4j.forge.audit.AuditActorProvider;
import io.github.foundry4j.forge.audit.AuditProperties;
import io.github.foundry4j.forge.audit.AuditRecorder;
import io.github.foundry4j.forge.audit.DefaultAuditActorProvider;
import io.github.foundry4j.forge.audit.ForgeAuditModule;
import io.github.foundry4j.forge.audit.OperationLogWriter;
import io.github.foundry4j.forge.audit.aspect.OperationLogAspect;
import io.github.foundry4j.forge.audit.repository.OperationLogRepository;
import io.github.foundry4j.forge.data.audit.AuditSupport;
import io.github.foundry4j.forge.data.autoconfigure.ForgeDataAutoConfiguration;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/// Wires the audit trail and the `@OperationLog` aspect.
@AutoConfiguration(after = ForgeDataAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class, JdbcTemplate.class})
@EntityScan(basePackageClasses = ForgeAuditModule.class)
@EnableJpaRepositories(basePackageClasses = ForgeAuditModule.class)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    /// Resolves the actor of audit records.
    @Bean
    @ConditionalOnMissingBean
    public AuditActorProvider forgeAuditActorProvider() {
        return new DefaultAuditActorProvider();
    }

    /// Persists entity change events; an application provided [AuditSupport] wins.
    @Bean
    @ConditionalOnMissingBean(AuditSupport.class)
    public AuditRecorder forgeAuditRecorder(JdbcTemplate jdbcTemplate,
                                            AuditActorProvider actorProvider,
                                            ObjectProvider<ObjectMapper> objectMapper,
                                            AuditProperties properties) {
        ObjectMapper mapper = objectMapper.getIfAvailable(() -> JsonMapper.builder().build());
        return new AuditRecorder(jdbcTemplate, actorProvider, mapper, properties);
    }

    /// Writes `@OperationLog` rows in an isolated transaction.
    @Bean
    @ConditionalOnMissingBean
    public OperationLogWriter forgeOperationLogWriter(OperationLogRepository repository) {
        return new OperationLogWriter(repository);
    }

    /// Records methods annotated with `@OperationLog`.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Aspect.class)
    public OperationLogAspect forgeOperationLogAspect(OperationLogWriter writer,
                                                      AuditActorProvider actorProvider) {
        return new OperationLogAspect(writer, actorProvider);
    }
}
