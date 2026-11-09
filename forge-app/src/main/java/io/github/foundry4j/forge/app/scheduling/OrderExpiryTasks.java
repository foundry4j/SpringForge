package io.github.foundry4j.forge.app.scheduling;

import io.github.foundry4j.forge.app.order.OrderService;
import io.github.foundry4j.forge.scheduling.ForgeScheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/// Periodically expires unpaid orders.
///
/// The method is picked up by [io.github.foundry4j.forge.scheduling.ForgeScheduledRegistrar]
/// when the context is refreshed; no XML, no manual scheduler wiring.
@Component
public class OrderExpiryTasks {

    private static final Duration MAX_AGE = Duration.ofMinutes(30);

    private final OrderService orderService;

    /// Creates the tasks.
    public OrderExpiryTasks(OrderService orderService) {
        this.orderService = orderService;
    }

    /// Expires orders that were not paid within thirty minutes.
    @ForgeScheduled(cron = "0 */10 * * * ?", name = "order-expiry")
    public void expireOverdueOrders() {
        orderService.expireOverdue(MAX_AGE);
    }
}
