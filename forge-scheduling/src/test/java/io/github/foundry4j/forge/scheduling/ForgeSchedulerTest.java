package io.github.foundry4j.forge.scheduling;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.utils.mapper.jackson3.Jackson3JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests the JobRunr facade against an in-memory storage provider.
class ForgeSchedulerTest {

    private InMemoryStorageProvider storage;
    private ForgeScheduler forgeScheduler;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorageProvider();
        storage.setJobMapper(new JobMapper(new Jackson3JsonMapper()));
        forgeScheduler = new ForgeScheduler(new JobScheduler(storage), storage,
                new ForgeSchedulingProperties(true, "test-group", true));
    }

    @Test
    void registersRecurringCronJobs() {
        String id = forgeScheduler.scheduleCron("probe", "0 0 * * * *",
                () -> ForgeJobDispatcher.invoke("probe", "run"));

        assertThat(id).isEqualTo("test-group.probe");
        assertThat(forgeScheduler.exists("probe", "test-group")).isTrue();
    }

    @Test
    void registersFixedDelayJobs() {
        String id = forgeScheduler.scheduleFixedDelay("fixed", Duration.ofHours(1),
                () -> ForgeJobDispatcher.invoke("probe", "run"));

        assertThat(id).isEqualTo("test-group.fixed");
        assertThat(forgeScheduler.exists("test-group.fixed")).isTrue();
    }

    @Test
    void cancelsRecurringJobs() {
        forgeScheduler.scheduleCron("probe", "0 0 * * * *",
                () -> ForgeJobDispatcher.invoke("probe", "run"));

        assertThat(forgeScheduler.cancel("probe", "test-group")).isTrue();
        assertThat(forgeScheduler.exists("probe", "test-group")).isFalse();
        assertThat(forgeScheduler.cancel("probe", "test-group")).isFalse();
    }

    @Test
    void enqueuesImmediateJobs() {
        JobId id = forgeScheduler.enqueue(() -> ForgeJobDispatcher.invoke("probe", "run"));

        assertThat(storage.getJobById(id.asUUID())).isNotNull();
    }

    @Test
    void schedulesOneShotJobs() {
        JobId id = forgeScheduler.schedule(Instant.now().plusSeconds(3600),
                () -> ForgeJobDispatcher.invoke("probe", "run"));

        assertThat(storage.getJobById(id.asUUID())).isNotNull();
    }
}
