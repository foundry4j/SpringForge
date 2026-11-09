package io.github.foundry4j.forge.rpc.client;

import io.github.foundry4j.forge.rpc.RpcTracing;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/// Client interceptor propagating the current MDC trace id as `x-trace-id` metadata.
///
/// An explicitly supplied header is never overwritten.
public class RpcClientTraceInterceptor implements ClientInterceptor {

    private final boolean enabled;

    /// Creates an enabled interceptor.
    public RpcClientTraceInterceptor() {
        this(true);
    }

    /// Creates an interceptor with an explicit switch.
    public RpcClientTraceInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        String traceId = enabled ? RpcTracing.currentTraceId() : null;
        if (traceId == null || traceId.isBlank()) {
            return next.newCall(method, callOptions);
        }
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (headers.get(RpcTracing.TRACE_ID_KEY) == null) {
                    headers.put(RpcTracing.TRACE_ID_KEY, traceId);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
