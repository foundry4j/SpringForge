package io.github.foundry4j.forge.web.idempotent;

import io.github.foundry4j.forge.cache.store.LocalKeyValueStore;
import io.github.foundry4j.forge.core.exception.BusinessException;
import io.github.foundry4j.forge.core.exception.ConflictException;
import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for the idempotency aspect.
class IdempotencyAspectTest {

    private SampleService service;

    @BeforeEach
    void setUp() {
        var factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(new IdempotencyAspect(new LocalKeyValueStore(100), new ExpressionEvaluator()));
        service = factory.getProxy();
    }

    @Test
    void allowsTheFirstCallAndRejectsDuplicates() {
        assertThat(service.create("key-1")).isEqualTo("created-key-1");
        assertThatThrownBy(() -> service.create("key-1")).isInstanceOf(ConflictException.class);
        assertThat(service.create("key-2")).isEqualTo("created-key-2");
    }

    @Test
    void releasesTheKeyWhenTheCallFails() {
        assertThatThrownBy(() -> service.failing("key-3")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.failing("key-3"))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsCallsWithoutAKey() {
        assertThatThrownBy(service::withoutKey)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Idempotency-Key");
    }

    /// Service used to exercise the aspect through a real AOP proxy.
    static class SampleService {

        @Idempotent(key = "#id", ttlSeconds = 60)
        public String create(String id) {
            return "created-" + id;
        }

        @Idempotent(key = "#id", ttlSeconds = 60)
        public String failing(String id) {
            throw new IllegalStateException("boom");
        }

        @Idempotent(ttlSeconds = 60)
        public String withoutKey() {
            return "never";
        }
    }
}
