package io.github.foundry4j.forge.rpc.server;

import io.github.foundry4j.forge.rpc.RpcTracing;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.MDC;

/// Server interceptor copying the `x-trace-id` metadata into the logging MDC.
///
/// The MDC entry is restored around every listener callback, so RPC and HTTP logs share
/// the same correlation identifier.
public class RpcServerTraceInterceptor implements ServerInterceptor {

    private final boolean enabled;

    /// Creates an enabled interceptor.
    public RpcServerTraceInterceptor() {
        this(true);
    }

    /// Creates an interceptor with an explicit switch.
    public RpcServerTraceInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        String traceId = enabled ? headers.get(RpcTracing.TRACE_ID_KEY) : null;
        if (traceId == null || traceId.isBlank()) {
            return listener;
        }
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            @Override
            public void onMessage(ReqT message) {
                withTrace(traceId, () -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                withTrace(traceId, super::onHalfClose);
            }

            private void withTrace(String trace, Runnable action) {
                String previous = MDC.get(RpcTracing.TRACE_ID_MDC_KEY);
                MDC.put(RpcTracing.TRACE_ID_MDC_KEY, trace);
                try {
                    action.run();
                } finally {
                    if (previous == null) {
                        MDC.remove(RpcTracing.TRACE_ID_MDC_KEY);
                    } else {
                        MDC.put(RpcTracing.TRACE_ID_MDC_KEY, previous);
                    }
                }
            }
        };
    }
}
