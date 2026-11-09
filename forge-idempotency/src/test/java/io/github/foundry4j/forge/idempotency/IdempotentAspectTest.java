package io.github.foundry4j.forge.idempotency;

import io.github.foundry4j.forge.core.exception.ConflictException;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for the idempotency aspect.
class IdempotentAspectTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final InMemoryIdempotentStore store = new InMemoryIdempotentStore();
    private SampleService target;
    private SampleService service;

    @BeforeEach
    void setUp() {
        target = new SampleService();
        var factory = new AspectJProxyFactory(target);
        factory.addAspect(new IdempotentAspect(store,
                new ExpressionIdempotentKeyResolver(new ExpressionEvaluator(), objectMapper), objectMapper));
        service = factory.getProxy();
    }

    @Test
    void executesOnceAndReplaysTheCachedResult() {
        assertThat(service.create("A-1")).isEqualTo("created-A-1");
        assertThat(service.create("A-1")).isEqualTo("created-A-1");

        assertThat(target.executions.get()).isEqualTo(1);
    }

    @Test
    void releasesTheKeyWhenTheCallFails() {
        assertThatThrownBy(() -> service.failing("A-2")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.failing("A-2")).isInstanceOf(IllegalStateException.class);

        assertThat(target.executions.get()).isEqualTo(2);
    }

    @Test
    void rejectsDuplicatesWhileTheFirstCallIsProcessing() {
        store.tryAcquire(SampleService.class.getName() + "#create:order:A-3", 60);

        assertThatThrownBy(() -> service.create("A-3")).isInstanceOf(ConflictException.class);
    }

    /// Service used to exercise the aspect through a real AOP proxy.
    static class SampleService {

        private final AtomicInteger executions = new AtomicInteger();

        @Idempotent(key = "order:#id", ttlSeconds = 60)
        public String create(String id) {
            executions.incrementAndGet();
            return "created-" + id;
        }

        @Idempotent(key = "order:#id", ttlSeconds = 60)
        public String failing(String id) {
            executions.incrementAndGet();
            throw new IllegalStateException("boom");
        }
    }
}
