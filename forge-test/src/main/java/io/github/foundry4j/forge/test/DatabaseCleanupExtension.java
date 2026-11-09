package io.github.foundry4j.forge.test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

/// JUnit extension behind [CleanDatabase].
///
/// It resolves the [EntityManager] and the transaction manager from the Spring test
/// application context and deletes all rows inside a single transaction.
public class DatabaseCleanupExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        EntityManager entityManager = applicationContext.getBean(EntityManager.class);
        PlatformTransactionManager transactionManager = applicationContext.getBean(PlatformTransactionManager.class);

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();
            for (EntityType<?> entity : entities) {
                entityManager.createQuery("DELETE FROM " + entity.getName()).executeUpdate();
            }
        });
    }
}
