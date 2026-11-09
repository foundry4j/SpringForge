package io.github.foundry4j.forge.audit.entity;

import io.github.foundry4j.forge.data.audit.NotAudited;
import io.github.foundry4j.forge.data.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/// Persisted invocation log of a method annotated with
/// [io.github.foundry4j.forge.audit.annotation.OperationLog].
///
/// The entity is written in a separate transaction, so a failed business operation is
/// logged as well.
@Entity
@Table(name = "forge_operation_log", indexes = {
        @Index(name = "idx_operation_actor", columnList = "actor"),
        @Index(name = "idx_operation_occurred", columnList = "occurred_at")
})
@NotAudited
public class OperationLogEntry extends BaseEntity {

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "actor", length = 64)
    private String actor;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "target", length = 256)
    private String target;

    @Column(name = "arguments", length = 4000)
    private String arguments;

    @Column(name = "result", length = 4000)
    private String result;

    @Column(name = "error", length = 2000)
    private String error;

    @Column(name = "cost_millis", nullable = false)
    private long costMillis;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /// Creates an empty entity for JPA.
    protected OperationLogEntry() {
    }

    /// Creates a complete operation log entry.
    public OperationLogEntry(String traceId, String actor, String action, String target,
                             String arguments, String result, String error, long costMillis,
                             String clientIp, String userAgent, Instant occurredAt) {
        this.traceId = traceId;
        this.actor = actor;
        this.action = action;
        this.target = target;
        this.arguments = arguments;
        this.result = result;
        this.error = error;
        this.costMillis = costMillis;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.occurredAt = occurredAt;
    }

    /// Trace identifier of the request.
    public String getTraceId() {
        return traceId;
    }

    /// Actor who invoked the method.
    public String getActor() {
        return actor;
    }

    /// Business action name.
    public String getAction() {
        return action;
    }

    /// Class and method name.
    public String getTarget() {
        return target;
    }

    /// Truncated arguments.
    public String getArguments() {
        return arguments;
    }

    /// Truncated result.
    public String getResult() {
        return result;
    }

    /// Failure message, `null` for a successful call.
    public String getError() {
        return error;
    }

    /// Duration in milliseconds.
    public long getCostMillis() {
        return costMillis;
    }

    /// Client address, if the call came from a servlet request.
    public String getClientIp() {
        return clientIp;
    }

    /// User agent of the request.
    public String getUserAgent() {
        return userAgent;
    }

    /// Time at which the method was invoked.
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
