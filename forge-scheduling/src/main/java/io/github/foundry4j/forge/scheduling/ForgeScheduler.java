package io.github.foundry4j.forge.scheduling;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/// High level facade over the Quartz scheduler.
///
/// Instants are expressed with `java.time` and durations with [Duration]; Quartz's
/// `java.util.Date` never leaks into application code.
public class ForgeScheduler {

    private final Scheduler scheduler;
    private final ForgeSchedulingProperties properties;

    /// Creates the facade.
    public ForgeScheduler(Scheduler scheduler, ForgeSchedulingProperties properties) {
        this.scheduler = scheduler;
        this.properties = properties;
    }

    /// Schedules a cron job in the default group.
    public void scheduleCron(String name, Class<? extends Job> jobType, String cron, Map<String, Object> data) {
        scheduleCron(name, properties.group(), jobType, cron, data);
    }

    /// Schedules a cron job.
    ///
    /// @param name    unique job name inside `group`
    /// @param group   job group
    /// @param jobType job implementation
    /// @param cron    Quartz cron expression
    /// @param data    job data made available through [ForgeJobContext#data()]
    public void scheduleCron(String name, String group, Class<? extends Job> jobType,
                             String cron, Map<String, Object> data) {
        JobDetail detail = jobDetail(name, group, jobType, data);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(name, group))
                .forJob(detail)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
        schedule(detail, trigger);
    }

    /// Schedules a job that runs forever with a fixed delay between executions.
    public void scheduleFixedDelay(String name, Class<? extends Job> jobType,
                                   Duration delay, Map<String, Object> data) {
        scheduleFixedDelay(name, properties.group(), jobType, delay, data);
    }

    /// Schedules a job that runs forever with a fixed delay between executions.
    public void scheduleFixedDelay(String name, String group, Class<? extends Job> jobType,
                                   Duration delay, Map<String, Object> data) {
        JobDetail detail = jobDetail(name, group, jobType, data);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(name, group))
                .forJob(detail)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(delay.toMillis())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
        schedule(detail, trigger);
    }

    /// Schedules a one shot job.
    public void scheduleOnce(String name, Class<? extends Job> jobType,
                             Instant fireAt, Map<String, Object> data) {
        scheduleOnce(name, properties.group(), jobType, fireAt, data);
    }

    /// Schedules a one shot job at the given instant.
    public void scheduleOnce(String name, String group, Class<? extends Job> jobType,
                             Instant fireAt, Map<String, Object> data) {
        JobDetail detail = jobDetail(name, group, jobType, data);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(name, group))
                .forJob(detail)
                .startAt(Date.from(fireAt))
                .build();
        schedule(detail, trigger);
    }

    /// Fires the job immediately, without waiting for its next scheduled time.
    public void triggerNow(String name, String group) {
        try {
            scheduler.triggerJob(jobKey(name, group));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Cannot trigger job [%s.%s]".formatted(group, name), exception);
        }
    }

    /// Deletes the job and all of its triggers.
    ///
    /// @return `true` when the job existed
    public boolean cancel(String name, String group) {
        try {
            return scheduler.deleteJob(jobKey(name, group));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Cannot cancel job [%s.%s]".formatted(group, name), exception);
        }
    }

    /// Pauses the job without deleting it.
    public void pause(String name, String group) {
        try {
            scheduler.pauseJob(jobKey(name, group));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Cannot pause job [%s.%s]".formatted(group, name), exception);
        }
    }

    /// Resumes a paused job.
    public void resume(String name, String group) {
        try {
            scheduler.resumeJob(jobKey(name, group));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Cannot resume job [%s.%s]".formatted(group, name), exception);
        }
    }

    /// Whether the job is registered.
    public boolean exists(String name, String group) {
        try {
            return scheduler.checkExists(jobKey(name, group));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Cannot inspect job [%s.%s]".formatted(group, name), exception);
        }
    }

    /// The wrapped Quartz scheduler, for advanced use cases.
    public Scheduler scheduler() {
        return scheduler;
    }

    private void schedule(JobDetail detail, Trigger trigger) {
        try {
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Cannot schedule job [%s]".formatted(detail.getKey()), exception);
        }
    }

    private JobDetail jobDetail(String name, String group, Class<? extends Job> jobType,
                                Map<String, Object> data) {
        return JobBuilder.newJob(jobType)
                .withIdentity(jobKey(name, group))
                .usingJobData(new JobDataMap(data == null ? Map.of() : data))
                .requestRecovery(true)
                .build();
    }

    private JobKey jobKey(String name, String group) {
        return JobKey.jobKey(name, group);
    }

    private TriggerKey triggerKey(String name, String group) {
        return TriggerKey.triggerKey(name + ".trigger", group);
    }
}
