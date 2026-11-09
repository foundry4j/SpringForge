package io.github.foundry4j.forge.scheduling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies that JobRunr jobs can dispatch back into Spring beans.
class ForgeJobDispatcherTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.registerBean("probe", Probe.class);
        context.refresh();
        ForgeJobDispatcher.bind(context);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void invokesNoArgumentMethodsOnBeans() {
        ForgeJobDispatcher.invoke("probe", "run");

        assertThat(context.getBean(Probe.class).calls).isEqualTo(1);
    }

    @Test
    void reportsAMissingMethod() {
        assertThatThrownBy(() -> ForgeJobDispatcher.invoke("probe", "missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer exists");
    }

    @Test
    void propagatesFailuresOfTheScheduledMethod() {
        assertThatThrownBy(() -> ForgeJobDispatcher.invoke("probe", "fail"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed")
                .hasRootCauseMessage("boom");
    }

    /// Bean recording how often it was invoked.
    public static class Probe {

        private int calls;

        /// Records a successful execution.
        public void run() {
            calls++;
        }

        /// Fails on purpose.
        public void fail() {
            throw new IllegalArgumentException("boom");
        }
    }
}
