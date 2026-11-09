package io.github.foundry4j.forge.audit.aspect;

import io.github.foundry4j.forge.audit.AuditActorProvider;
import io.github.foundry4j.forge.audit.OperationLogWriter;
import io.github.foundry4j.forge.audit.annotation.OperationLog;
import io.github.foundry4j.forge.audit.entity.OperationLogEntry;
import io.github.foundry4j.forge.core.context.ForgeContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Arrays;

/// Captures methods annotated with [OperationLog].
@Aspect
public class OperationLogAspect {

    private static final Logger LOG = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int MAX_TEXT = 3900;

    private final OperationLogWriter writer;
    private final AuditActorProvider actorProvider;

    /// Creates the aspect.
    public OperationLogAspect(OperationLogWriter writer, AuditActorProvider actorProvider) {
        this.writer = writer;
        this.actorProvider = actorProvider;
    }

    /// Records the invocation and rethrows any failure unchanged.
    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            write(joinPoint, operationLog, result, null, elapsedMillis(start));
            return result;
        } catch (Throwable ex) {
            write(joinPoint, operationLog, null, describe(ex), elapsedMillis(start));
            throw ex;
        }
    }

    private void write(ProceedingJoinPoint joinPoint, OperationLog annotation,
                       Object result, String error, long costMillis) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String action = annotation.value().isBlank() ? signature.getMethod().getName() : annotation.value();
        String target = joinPoint.getTarget() == null
                ? signature.getDeclaringType().getSimpleName()
                : joinPoint.getTarget().getClass().getSimpleName();
        String arguments = annotation.recordArguments() ? truncate(Arrays.deepToString(joinPoint.getArgs())) : null;
        String resultText = annotation.recordResult() && error == null ? truncate(String.valueOf(result)) : null;
        HttpServletRequest request = currentRequest();

        OperationLogEntry entry = new OperationLogEntry(
                ForgeContext.traceId(),
                actorProvider.currentActor(),
                action,
                target + "#" + signature.getMethod().getName(),
                arguments,
                resultText,
                truncate(error),
                costMillis,
                request == null ? null : request.getRemoteAddr(),
                request == null ? null : truncate(request.getHeader("User-Agent")),
                Instant.now());
        try {
            writer.write(entry);
        } catch (RuntimeException ex) {
            LOG.warn("Cannot persist operation log for {}", action, ex);
        }
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }

    private long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private String describe(Throwable ex) {
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT) + "...";
    }
}
