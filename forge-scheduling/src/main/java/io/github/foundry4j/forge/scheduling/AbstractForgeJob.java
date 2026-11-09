package io.github.foundry4j.forge.scheduling;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/// Convenience base class for Quartz jobs.
///
/// Subclasses only implement [#execute(ForgeJobContext)] with plain Java types; the
/// adapter converts checked exceptions into a [JobExecutionException] so that Quartz
/// records the failure.
public abstract class AbstractForgeJob implements Job {

    /// {@inheritDoc}
    @Override
    public final void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            execute(ForgeJobContext.of(context));
        } catch (JobExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JobExecutionException(exception);
        }
    }

    /// Runs the job logic.
    ///
    /// @param context immutable job context
    /// @throws Exception when the job failed and should be reported to Quartz
    protected abstract void execute(ForgeJobContext context) throws Exception;
}
