package io.github.foundry4j.forge.app.order;

/// Lifecycle of a sample order.
public enum OrderStatus {

    /// Order was created and awaits payment.
    CREATED,

    /// Order was paid.
    PAID,

    /// Order was cancelled by the customer.
    CANCELLED,

    /// Order was not paid in time and expired.
    EXPIRED
}
