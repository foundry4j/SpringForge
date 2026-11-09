package io.github.foundry4j.forge.idempotency;

import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the default expression based key resolver.
class ExpressionIdempotentKeyResolverTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final ExpressionIdempotentKeyResolver resolver =
            new ExpressionIdempotentKeyResolver(new ExpressionEvaluator(), objectMapper);
    private final SampleService target = new SampleService();

    @Test
    void evaluatesPlaceholdersAgainstTheArguments() throws Exception {
        assertThat(resolve("create", new CreateOrder("A-1")))
                .endsWith(SampleService.class.getName() + "#create:order:A-1");
    }

    @Test
    void supportsIndexedParameters() throws Exception {
        assertThat(resolve("createIndexed", new CreateOrder("A-2"))).endsWith(":order:A-2");
    }

    @Test
    void derivesTheKeyFromArgumentsWhenNoExpressionIsConfigured() throws Exception {
        assertThat(resolve("createDefault", new CreateOrder("A-3"))).contains("\"orderNo\":\"A-3\"");
    }

    private String resolve(String methodName, CreateOrder request) throws Exception {
        Method method = SampleService.class.getMethod(methodName, CreateOrder.class);
        return resolver.resolve(method.getAnnotation(Idempotent.class), method, target, new Object[]{request});
    }

    /// Request payload with a nested field used by the expressions.
    record CreateOrder(String orderNo) {
    }

    /// Service declaring the key expressions.
    static class SampleService {

        @Idempotent(key = "order:#request.orderNo", ttlSeconds = 60)
        public String create(CreateOrder request) {
            return "ok";
        }

        @Idempotent(key = "order:#p0.orderNo", ttlSeconds = 60)
        public String createIndexed(CreateOrder request) {
            return "ok";
        }

        @Idempotent(ttlSeconds = 60)
        public String createDefault(CreateOrder request) {
            return "ok";
        }
    }
}
