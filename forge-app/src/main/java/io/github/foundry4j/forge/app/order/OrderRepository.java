package io.github.foundry4j.forge.app.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// Repository of the sample orders.
public interface OrderRepository extends JpaRepository<Order, Long> {

    /// Finds an order by its business number.
    Optional<Order> findByOrderNo(String orderNo);

    /// Lists orders in the given status.
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /// Lists orders in the given status created before the threshold.
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant threshold);
}
