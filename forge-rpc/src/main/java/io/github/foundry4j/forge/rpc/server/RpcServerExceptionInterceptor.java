package io.github.foundry4j.forge.rpc.server;

import io.github.foundry4j.forge.core.exception.BusinessException;
import io.github.foundry4j.forge.rpc.RpcErrors;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/// Server interceptor converting thrown exceptions into gRPC status codes.
///
/// A [BusinessException] is mapped by its HTTP status segment and its stable error code is
/// written to the `x-error-code` trailer; every other exception becomes `INTERNAL` without
/// leaking internals. Only synchronous throws are covered, which is what plain gRPC stubs
/// produce.
public class RpcServerExceptionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            @Override
            public void onMessage(ReqT message) {
                try {
                    super.onMessage(message);
                } catch (Throwable throwable) {
                    close(call, throwable);
                }
            }

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Throwable throwable) {
                    close(call, throwable);
                }
            }

            @Override
            public void onReady() {
                try {
                    super.onReady();
                } catch (Throwable throwable) {
                    close(call, throwable);
                }
            }
        };
    }

    private void close(ServerCall<?, ?> call, Throwable throwable) {
        Metadata trailers = new Metadata();
        if (throwable instanceof BusinessException business) {
            trailers.put(RpcErrors.ERROR_CODE_KEY, business.errorCode().code());
        }
        call.close(RpcErrors.toStatus(throwable), trailers);
    }
}
