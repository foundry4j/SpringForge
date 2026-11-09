package io.github.foundry4j.forge.data;

import io.github.foundry4j.forge.core.context.ForgeContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that the JPA base layer provides auditing, tenant isolation and soft delete.
@SpringBootTest(classes = ForgeDataTestApplication.class)
class ForgeDataBaseTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void fillsAuditColumnsAndTenant() {
        var caller = new ForgeContext.Caller("tenant-a", "user-1", "trace-1");

        ForgeContext.runAs(caller, () -> {
            Long id = transactionTemplate.execute(_ -> {
                var order = new TestOrder("A-1");
                entityManager.persist(order);
                return order.getId();
            });

            TestOrder loaded = transactionTemplate.execute(_ -> entityManager.find(TestOrder.class, id));

            assertThat(loaded).isNotNull();
            assertThat(loaded.getTenantId()).isEqualTo("tenant-a");
            assertThat(loaded.getCreatedBy()).isEqualTo("user-1");
            assertThat(loaded.getCreatedAt()).isNotNull();
            assertThat(loaded.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void hidesRowsOfOtherTenants() throws Exception {
        Long id = ForgeContext.callAs(new ForgeContext.Caller("tenant-b", "user-2", "trace-2"),
                () -> transactionTemplate.execute(_ -> {
                    var order = new TestOrder("B-1");
                    entityManager.persist(order);
                    return order.getId();
                }));

        ForgeContext.runAs(new ForgeContext.Caller("tenant-a", "user-1", "trace-1"), () -> {
            TestOrder loaded = transactionTemplate.execute(_ -> entityManager.find(TestOrder.class, id));
            assertThat(loaded).isNull();
        });

        ForgeContext.runAs(new ForgeContext.Caller("tenant-b", "user-2", "trace-2"), () -> {
            TestOrder loaded = transactionTemplate.execute(_ -> entityManager.find(TestOrder.class, id));
            assertThat(loaded).isNotNull();
        });
    }

    @Test
    void softDeletesRows() {
        Long id = transactionTemplate.execute(_ -> {
            var order = new TestOrder("C-1");
            entityManager.persist(order);
            return order.getId();
        });

        transactionTemplate.executeWithoutResult(_ -> {
            TestOrder order = entityManager.find(TestOrder.class, id);
            entityManager.remove(order);
            entityManager.flush();
        });

        TestOrder loaded = transactionTemplate.execute(_ -> entityManager.find(TestOrder.class, id));
        Integer rows = jdbcTemplate.queryForObject("select count(*) from test_order where id = ?", Integer.class, id);

        assertThat(loaded).isNull();
        assertThat(rows).isEqualTo(1);
    }
}
