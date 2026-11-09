package io.github.foundry4j.forge.app;

import io.github.foundry4j.forge.app.inventory.InventoryGateway;
import io.github.foundry4j.forge.app.order.CreateOrderRequest;
import io.github.foundry4j.forge.app.order.OrderService;
import io.github.foundry4j.forge.app.order.OrderView;
import io.github.foundry4j.forge.core.context.ForgeContext;
import io.github.foundry4j.forge.data.dict.DictService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/// Boots the whole application against H2 and exercises the main building blocks.
@SpringBootTest
class SpringForgeApplicationTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private DictService dictService;

    @Autowired
    private InventoryGateway inventoryGateway;

    @Test
    void contextLoads() {
        assertThat(orderService).isNotNull();
    }

    @Test
    void createsAndReadsAnOrderInsideATenant() {
        ForgeContext.runAs(new ForgeContext.Caller("default", "tester", null), () -> {
            OrderView created = orderService.create(
                    new CreateOrderRequest("Ada Lovelace", "13800138000", new BigDecimal("99.90"), "demo"));
            assertThat(created.orderNo()).startsWith("ORD-");
            assertThat(created.tenantId()).isEqualTo("default");
            assertThat(orderService.get(created.id()).customerName()).isEqualTo("Ada Lovelace");
        });
    }

    @Test
    void loadsSeededDictionaryEntries() {
        assertThat(dictService.items("order_status"))
                .extracting(item -> item.value())
                .contains("CREATED", "PAID", "CANCELLED", "EXPIRED");
    }

    @Test
    void fallsBackWhenInventoryIsUnavailable() {
        assertThat(inventoryGateway.stock(42L)).isZero();
    }
}
