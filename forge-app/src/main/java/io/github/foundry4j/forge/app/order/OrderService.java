package io.github.foundry4j.forge.app.order;

import io.github.foundry4j.forge.core.exception.Errors;
import io.github.foundry4j.forge.core.result.PageResult;
import io.github.foundry4j.forge.core.support.Ids;
import io.github.foundry4j.forge.data.support.PageResults;
import io.github.foundry4j.forge.outbox.OutboxRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/// Application service of the sample order aggregate.
///
/// It demonstrates the typical forge request flow: validation happens in the controller,
/// tenant isolation in Hibernate, auditing on persist and the domain event is written to
/// the outbox inside the same transaction.
@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final OutboxRecorder outbox;

    /// Creates the service.
    public OrderService(OrderRepository repository, OutboxRecorder outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    /// Creates an order and records an `order.created` event.
    @Transactional
    public OrderView create(CreateOrderRequest request) {
        Order order = new Order();
        order.setOrderNo("ORD-" + Ids.compactId());
        order.setCustomerName(request.customerName());
        order.setPhone(request.phone());
        order.setAmount(request.amount());
        order.setRemark(request.remark());
        repository.save(order);
        OrderView view = OrderView.from(order);
        outbox.record("order.created", "order.created", view, Map.of("orderNo", order.getOrderNo()));
        return view;
    }

    /// Returns a single order.
    @Transactional(readOnly = true)
    public OrderView get(long id) {
        return repository.findById(id)
                .map(OrderView::from)
                .orElseThrow(() -> Errors.notFound("Order", id));
    }

    /// Lists orders, optionally filtered by status.
    @Transactional(readOnly = true)
    public PageResult<OrderView> list(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Order> result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return PageResults.of(result).map(OrderView::from);
    }

    /// Marks an order as paid.
    @Transactional
    public OrderView pay(long id) {
        Order order = require(id);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw Errors.conflict("Order [%d] is not awaiting payment".formatted(id));
        }
        order.setStatus(OrderStatus.PAID);
        OrderView view = OrderView.from(order);
        outbox.record("order.paid", "order.paid", view, Map.of("orderNo", order.getOrderNo()));
        return view;
    }

    /// Cancels an order that was not paid yet.
    @Transactional
    public OrderView cancel(long id) {
        Order order = require(id);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw Errors.conflict("Order [%d] cannot be cancelled".formatted(id));
        }
        order.setStatus(OrderStatus.CANCELLED);
        OrderView view = OrderView.from(order);
        outbox.record("order.cancelled", "order.cancelled", view, Map.of("orderNo", order.getOrderNo()));
        return view;
    }

    /// Soft deletes an order; the row stays in the database but disappears from queries.
    @Transactional
    public void delete(long id) {
        repository.delete(require(id));
    }

    /// Expires unpaid orders older than `age` and records one event per order.
    ///
    /// @return number of expired orders
    @Transactional
    public int expireOverdue(Duration age) {
        Instant threshold = Instant.now().minus(age);
        List<Order> overdue = repository.findByStatusAndCreatedAtBefore(OrderStatus.CREATED, threshold);
        for (Order order : overdue) {
            order.setStatus(OrderStatus.EXPIRED);
            outbox.record("order.expired", "order.expired", OrderView.from(order),
                    Map.of("orderNo", order.getOrderNo()));
        }
        if (!overdue.isEmpty()) {
            LOG.info("Expired {} overdue orders", overdue.size());
        }
        return overdue.size();
    }

    private Order require(long id) {
        return repository.findById(id)
                .orElseThrow(() -> Errors.notFound("Order", id));
    }
}
