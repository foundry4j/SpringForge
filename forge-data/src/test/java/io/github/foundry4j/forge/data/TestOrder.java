package io.github.foundry4j.forge.data;

import io.github.foundry4j.forge.data.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/// Entity used to verify auditing, multi-tenancy and soft deletion.
@Entity
@Table(name = "test_order")
public class TestOrder extends TenantEntity {

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    protected TestOrder() {
    }

    public TestOrder(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderNo() {
        return orderNo;
    }
}
