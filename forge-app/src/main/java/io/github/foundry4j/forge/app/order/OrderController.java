package io.github.foundry4j.forge.app.order;

import io.github.foundry4j.forge.core.result.PageResult;
import io.github.foundry4j.forge.idempotency.Idempotent;
import io.github.foundry4j.forge.ratelimit.RateLimit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// HTTP facade of the order aggregate.
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    /// Creates the controller.
    public OrderController(OrderService service) {
        this.service = service;
    }

    /// Creates an order.
    ///
    /// The operation is protected twice: `Idempotency-Key` prevents duplicated
    /// submissions and the rate limit caps how many orders a client may create per
    /// minute.
    @PostMapping
    @Idempotent(ttlSeconds = 600)
    @RateLimit(permits = 30, windowSeconds = 60)
    public OrderView create(@Valid @RequestBody CreateOrderRequest request) {
        return service.create(request);
    }

    /// Returns a single order.
    @GetMapping("/{id}")
    public OrderView get(@PathVariable long id) {
        return service.get(id);
    }

    /// Lists orders with pagination.
    @GetMapping
    public PageResult<OrderView> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return service.list(status, page, size);
    }

    /// Pays an order.
    @PostMapping("/{id}/pay")
    public OrderView pay(@PathVariable long id) {
        return service.pay(id);
    }

    /// Cancels an order.
    @PostMapping("/{id}/cancel")
    public OrderView cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    /// Soft deletes an order.
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
