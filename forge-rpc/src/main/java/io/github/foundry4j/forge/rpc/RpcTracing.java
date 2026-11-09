package io.github.foundry4j.forge.rpc;

import io.grpc.Metadata;
import org.slf4j.MDC;

/// Bridges the gRPC `x-trace-id` header and the logging MDC used by the web layer.
///
/// Both sides use the same `traceId` MDC key, so HTTP requests and the RPC calls they
/// trigger end up in the same log trail.
public final class RpcTracing {

    /// MDC key shared with the web request context filter.
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /// gRPC metadata key carrying the trace identifier.
    public static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    private RpcTracing() {
    }

    /// Returns the trace id bound to the current thread, or `null`.
    public static String currentTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }
}
