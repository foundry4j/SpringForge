package io.github.foundry4j.forge.app.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// Exposes the simulated inventory lookup.
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryGateway gateway;

    /// Creates the controller.
    public InventoryController(InventoryGateway gateway) {
        this.gateway = gateway;
    }

    /// Returns the stock of a SKU; `degraded` is `true` when the fallback was used.
    @GetMapping("/{sku}")
    public StockView stock(@PathVariable long sku) {
        int available = gateway.stock(sku);
        return new StockView(sku, available, available == 0);
    }

    /// Response of the inventory endpoint.
    ///
    /// @param sku       requested SKU
    /// @param available available units
    /// @param degraded  whether the fallback produced the answer
    public record StockView(long sku, int available, boolean degraded) {
    }
}
