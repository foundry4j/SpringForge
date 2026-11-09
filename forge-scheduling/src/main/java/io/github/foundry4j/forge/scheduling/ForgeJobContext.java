package io.github.foundry4j.forge.scheduling;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Immutable view of a running job.
///
/// The context hides the Quartz types that a job normally has to deal with and exposes
/// only immutable values, so a job can be tested without a scheduler.
///
/// @param key               Quartz job key
/// @param name              job name
/// @param group             job group
/// @param fireTime          instant at which the job was actually fired
/// @param scheduledFireTime instant at which the job was supposed to fire
/// @param refireCount       number of automatic re-fires
/// @param data              merged job data map, may be empty
public record ForgeJobContext(
        JobKey key,
        String name,
        String group,
        Instant fireTime,
        Instant scheduledFireTime,
        int refireCount,
        Map<String, Object> data
) {

    /// Creates the context from a Quartz execution context.
    static ForgeJobContext of(JobExecutionContext context) {
        JobKey key = context.getJobDetail().getKey();
        JobDataMap merged = context.getMergedJobDataMap();
        Map<String, Object> data = Collections.unmodifiableMap(new LinkedHashMap<>(merged));
        return new ForgeJobContext(
                key,
                key.getName(),
                key.getGroup(),
                context.getFireTime().toInstant(),
                context.getScheduledFireTime().toInstant(),
                context.getRefireCount(),
                data);
    }
}
