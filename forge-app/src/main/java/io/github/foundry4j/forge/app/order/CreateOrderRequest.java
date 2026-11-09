package io.github.foundry4j.forge.app.order;

import io.github.foundry4j.forge.web.validation.Phone;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/// Request body used to create an order.
///
/// Constraints are declared on the record components; the web layer turns violations
/// into RFC 9457 problem details with a `violations` array.
///
/// @param customerName name of the customer
/// @param phone        optional mobile number
/// @param amount       positive amount with at most two decimals
/// @param remark       optional remark
public record CreateOrderRequest(
        @NotBlank @Size(max = 128) String customerName,
        @Phone @Size(max = 32) String phone,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Size(max = 512) String remark
) {
}
