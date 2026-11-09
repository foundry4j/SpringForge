package io.github.foundry4j.forge.core.context;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for the request scoped context.
class ForgeContextTest {

    @Test
    void exposesCallerValuesInsideScope() {
        var caller = new ForgeContext.Caller("acme", "42", "trace-1");
        ForgeContext.runAs(caller, () -> {
            assertThat(ForgeContext.tenantId()).contains("acme");
            assertThat(ForgeContext.userId()).contains("42");
            assertThat(ForgeContext.traceId()).isEqualTo("trace-1");
            assertThat(ForgeContext.requireCaller()).isEqualTo(caller);
        });
        assertThat(ForgeContext.current()).isEmpty();
    }

    @Test
    void usesFallbacksWhenUnbound() {
        assertThat(ForgeContext.traceId()).isEqualTo("-");
        assertThat(ForgeContext.tenantId()).isEmpty();
        assertThat(ForgeContext.userId()).isEmpty();
        assertThatThrownBy(ForgeContext::requireCaller).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void normalizesBlankValues() {
        var caller = new ForgeContext.Caller(" ", "", null);
        assertThat(caller.tenantId()).isNull();
        assertThat(caller.userId()).isNull();
        assertThat(caller.traceId()).isNull();
    }

    @Test
    void propagatesCheckedExceptionFromCallAs() {
        var caller = new ForgeContext.Caller("tenant", "user", "trace");
        assertThatThrownBy(() -> ForgeContext.callAs(caller, () -> {
            throw new IOException("boom");
        })).isInstanceOf(IOException.class);
    }
}
