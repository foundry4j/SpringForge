package io.github.foundry4j.forge.app.inventory;

import io.github.foundry4j.forge.resilience.FaultTolerant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/// Simulated remote inventory service.
///
/// The call always fails, which shows the circuit breaker, the retries and the fallback
/// in action: the endpoint keeps answering with a degraded result instead of an error.
@Service
public class InventoryGateway {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryGateway.class);

    /// Looks up the available stock of a SKU.
    @FaultTolerant(name = "inventory", fallback = "fallbackStock")
    public int stock(long sku) {
        throw new IllegalStateException("inventory service is unavailable for " + sku);
    }

    /// Degradation used when the remote call fails or the circuit is open.
    private int fallbackStock(long sku, Throwable cause) {
        LOG.warn("Falling back to empty stock for {}: {}", sku, cause.getMessage());
        return 0;
    }
}
