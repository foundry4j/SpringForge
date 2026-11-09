package io.github.foundry4j.forge.app.order;

import io.github.foundry4j.forge.data.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/// Sample order entity.
///
/// Extending [TenantEntity] gives the row a tenant discriminator, audit columns,
/// optimistic locking and soft deletion without any additional code.
@Entity
@Table(name = "forge_order", indexes = @Index(
        name = "idx_forge_order_tenant_status", columnList = "tenant_id,status"))
public class Order extends TenantEntity {

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @Column(name = "customer_name", nullable = false, length = 128)
    private String customerName;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "remark", length = 512)
    private String remark;

    /// Unique order number.
    public String getOrderNo() {
        return orderNo;
    }

    /// Sets the order number.
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /// Name of the customer.
    public String getCustomerName() {
        return customerName;
    }

    /// Sets the customer name.
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /// Contact phone of the customer.
    public String getPhone() {
        return phone;
    }

    /// Sets the contact phone.
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /// Order amount.
    public BigDecimal getAmount() {
        return amount;
    }

    /// Sets the order amount.
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /// Current order status.
    public OrderStatus getStatus() {
        return status;
    }

    /// Sets the order status.
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /// Free form remark.
    public String getRemark() {
        return remark;
    }

    /// Sets the remark.
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
