package io.github.foundry4j.forge.app.order;

import java.math.BigDecimal;
import java.time.Instant;

/// Read only representation of an order.
///
/// @param id           surrogate key
/// @param orderNo      business number
/// @param tenantId     owning tenant
/// @param customerName customer name
/// @param phone        contact phone
/// @param amount       order amount
/// @param status       current status
/// @param createdAt    creation timestamp
public record OrderView(
        Long id,
        String orderNo,
        String tenantId,
        String customerName,
        String phone,
        BigDecimal amount,
        OrderStatus status,
        Instant createdAt
) {

    /// Maps an entity to its read model.
    public static OrderView from(Order order) {
        return new OrderView(
                order.getId(),
                order.getOrderNo(),
                order.getTenantId(),
                order.getCustomerName(),
                order.getPhone(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt());
    }
}
