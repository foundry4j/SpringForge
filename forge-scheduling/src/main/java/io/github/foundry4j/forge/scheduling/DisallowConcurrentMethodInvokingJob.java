package io.github.foundry4j.forge.scheduling;

import org.quartz.DisallowConcurrentExecution;
import org.springframework.context.ApplicationContext;

/// [MethodInvokingJob] variant that never runs concurrently with itself.
///
/// Registered for every [ForgeScheduled] method that keeps the default
/// `concurrent = false`, which is the safe choice for jobs that mutate shared state.
@DisallowConcurrentExecution
public class DisallowConcurrentMethodInvokingJob extends MethodInvokingJob {

    /// Creates the job; instances are produced by [ForgeJobFactory].
    public DisallowConcurrentMethodInvokingJob(ApplicationContext context) {
        super(context);
    }
}
