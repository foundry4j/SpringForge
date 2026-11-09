package io.github.foundry4j.forge.web.error;

import io.github.foundry4j.forge.core.exception.Errors;
import io.github.foundry4j.forge.core.exception.TooManyRequestsException;
import io.github.foundry4j.forge.web.ForgeWebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the RFC 9457 problem detail translation.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(new ForgeWebProperties(null, null, null, null, null));

    @Test
    void mapsBusinessExceptionsToProblemDetails() {
        var detail = handler.handleBusiness(Errors.notFound("Order", 7L));

        assertThat(detail.getStatus()).isEqualTo(404);
        assertThat(detail.getProperties()).containsEntry("code", "COMMON_404");
        assertThat(detail.getType()).hasToString("urn:forge:error:COMMON_404");
        assertThat(detail.getDetail()).contains("Order");
    }

    @Test
    void exposesRetryAfterForRateLimits() {
        var detail = handler.handleBusiness(new TooManyRequestsException("limit reached", 30));

        assertThat(detail.getStatus()).isEqualTo(429);
        assertThat(detail.getProperties()).containsEntry("retryAfter", 30L);
    }

    @Test
    void mapsBindingErrorsToViolations() {
        var result = new BeanPropertyBindingResult(new SampleRequest(), "request");
        result.rejectValue("customerName", "NotBlank", "must not be blank");

        var detail = handler.handleBinding(new BindException(result));

        assertThat(detail.getStatus()).isEqualTo(400);
        assertThat((List<?>) detail.getProperties().get("violations")).hasSize(1);
    }

    /// Bean used to create binding errors.
    static class SampleRequest {

        private String customerName;

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        var detail = handler.handleUnexpected(new IllegalStateException("secret internals"));

        assertThat(detail.getStatus()).isEqualTo(500);
        assertThat(detail.getDetail()).isEqualTo("Internal server error");
        assertThat(detail.getDetail()).doesNotContain("secret internals");
    }
}
