package io.github.foundry4j.forge.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Verifies the HTTP contract: validation, idempotency, error mapping and wrapping.
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsInvalidOrdersWithFieldViolations() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"","phone":"123","amount":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void createsOrdersAndWrapsTheResponse() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "api-test-1")
                        .header("X-Tenant-Id", "acme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Ada","phone":"13800138000","amount":99.90}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.customerName").value("Ada"))
                .andExpect(jsonPath("$.data.tenantId").value("acme"))
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty());
    }

    @Test
    void rejectsDuplicatedIdempotentRequests() throws Exception {
        String body = """
                {"customerName":"Grace","amount":10.00}
                """;
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "api-test-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "api-test-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMON_409"));
    }

    @Test
    void servesDictionaryEntries() throws Exception {
        mockMvc.perform(get("/api/dicts/order_status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].value").value("CREATED"));
    }

    @Test
    void returnsProblemDetailForUnknownOrders() throws Exception {
        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }
}
