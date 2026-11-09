package io.github.foundry4j.forge.scheduling;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;

import java.time.Duration;
import java.time.Instant;

/// High level facade over JobRunr.
///
/// Jobs are described with [JobLambda] instances, which JobRunr persists and distributes
/// across all background job servers. Recurring jobs are identified by
/// `<group>.<name>`; JobRunr keeps them in its storage, so they survive restarts and are
/// shared by every node.
public class ForgeScheduler {

    private final JobScheduler jobScheduler;
    private final StorageProvider storageProvider;
    private final ForgeSchedulingProperties properties;

    /// Creates the facade.
    public ForgeScheduler(JobScheduler jobScheduler, StorageProvider storageProvider,
                          ForgeSchedulingProperties properties) {
        this.jobScheduler = jobScheduler;
        this.storageProvider = storageProvider;
        this.properties = properties;
    }

    /// Enqueues a job for immediate execution.
    public JobId enqueue(JobLambda job) {
        return jobScheduler.enqueue(job);
    }

    /// Schedules a one shot job at the given instant.
    public JobId schedule(Instant fireAt, JobLambda job) {
        return jobScheduler.schedule(fireAt, job);
    }

    /// Registers a recurring cron job in the default group.
    public String scheduleCron(String name, String cron, JobLambda job) {
        return scheduleCron(name, properties.group(), cron, job);
    }

    /// Registers a recurring cron job.
    ///
    /// @param name  job name, unique inside `group`
    /// @param group identifier prefix
    /// @param cron  JobRunr cron expression
    /// @param job   job body
    /// @return the identifier of the registered recurring job
    public String scheduleCron(String name, String group, String cron, JobLambda job) {
        return jobScheduler.scheduleRecurrently(jobId(name, group), cron, job);
    }

    /// Registers a recurring job that runs with a fixed interval.
    public String scheduleFixedDelay(String name, Duration interval, JobLambda job) {
        return scheduleFixedDelay(name, properties.group(), interval, job);
    }

    /// Registers a recurring job that runs with a fixed interval.
    public String scheduleFixedDelay(String name, String group, Duration interval, JobLambda job) {
        return jobScheduler.scheduleRecurrently(jobId(name, group), interval, job);
    }

    /// Deletes a recurring job.
    ///
    /// @return `true` when the job was registered
    public boolean cancel(String name, String group) {
        String id = jobId(name, group);
        if (!exists(id)) {
            return false;
        }
        storageProvider.deleteRecurringJob(id);
        return true;
    }

    /// Whether the recurring job is registered.
    public boolean exists(String name, String group) {
        return exists(jobId(name, group));
    }

    /// Whether a recurring job with the given identifier is registered.
    public boolean exists(String id) {
        return storageProvider.getRecurringJobs().stream()
                .anyMatch(job -> id.equals(job.getId()));
    }

    /// Identifier derived from the group and the job name.
    public String jobId(String name, String group) {
        return group + "." + name;
    }

    /// The wrapped JobRunr scheduler, for advanced use cases.
    public JobScheduler jobScheduler() {
        return jobScheduler;
    }
}
