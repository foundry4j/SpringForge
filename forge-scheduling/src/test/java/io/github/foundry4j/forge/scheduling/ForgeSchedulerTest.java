package io.github.foundry4j.forge.scheduling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests the scheduler facade against a real in-memory Quartz scheduler.
class ForgeSchedulerTest {

    private Scheduler scheduler;
    private ForgeScheduler forgeScheduler;

    @BeforeEach
    void setUp() throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        forgeScheduler = new ForgeScheduler(scheduler,
                new ForgeSchedulingProperties(true, "test-group", true));
    }

    @AfterEach
    void tearDown() throws SchedulerException {
        scheduler.shutdown(true);
    }

    @Test
    void schedulesAndTriggersAJob() throws Exception {
        ProbeJob.reset();
        forgeScheduler.scheduleCron("probe", ProbeJob.class, "0 0 0 1 1 ? 2099", Map.of("marker", "x"));

        assertThat(forgeScheduler.exists("probe", "test-group")).isTrue();

        forgeScheduler.triggerNow("probe", "test-group");

        assertThat(ProbeJob.fired.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ProbeJob.last.get().name()).isEqualTo("probe");
        assertThat(ProbeJob.last.get().group()).isEqualTo("test-group");
        assertThat(ProbeJob.last.get().data()).containsEntry("marker", "x");
    }

    @Test
    void cancelsOneShotJobs() {
        forgeScheduler.scheduleOnce("once", ProbeJob.class, Instant.now().plusSeconds(3600), Map.of());

        assertThat(forgeScheduler.exists("once", "test-group")).isTrue();
        assertThat(forgeScheduler.cancel("once", "test-group")).isTrue();
        assertThat(forgeScheduler.exists("once", "test-group")).isFalse();
    }

    @Test
    void pausesAndResumesRecurringJobs() {
        forgeScheduler.scheduleFixedDelay("fixed", ProbeJob.class, Duration.ofHours(1), Map.of());

        forgeScheduler.pause("fixed", "test-group");
        forgeScheduler.resume("fixed", "test-group");

        assertThat(forgeScheduler.exists("fixed", "test-group")).isTrue();
    }

    /// Job recording its execution context.
    public static class ProbeJob extends AbstractForgeJob {

        private static CountDownLatch fired;
        private static AtomicReference<ForgeJobContext> last;

        static void reset() {
            fired = new CountDownLatch(1);
            last = new AtomicReference<>();
        }

        @Override
        protected void execute(ForgeJobContext context) {
            last.set(context);
            fired.countDown();
        }
    }
}
