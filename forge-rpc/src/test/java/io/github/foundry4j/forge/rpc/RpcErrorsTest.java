package io.github.foundry4j.forge.rpc;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests the HTTP to gRPC error mapping.
class RpcErrorsTest {

    @Test
    void mapsBusinessCodesByHttpStatus() {
        assertThat(RpcErrors.toStatusCode(404)).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(RpcErrors.toStatusCode(403)).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(RpcErrors.toStatusCode(401)).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(RpcErrors.toStatusCode(409)).isEqualTo(Status.Code.ABORTED);
    }

    @Test
    void convertsBusinessExceptionsToStatus() {
        Status status = RpcErrors.toStatus(new BusinessException(CommonErrorCode.NOT_FOUND, "Order missing"));

        assertThat(status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(status.getDescription()).isEqualTo("Order missing");
    }

    @Test
    void restoresTheOriginalCodeFromTheTrailer() {
        Metadata trailers = new Metadata();
        trailers.put(RpcErrors.ERROR_CODE_KEY, "COMMON_404");
        StatusRuntimeException failed = Status.NOT_FOUND
                .withDescription("Order missing")
                .asRuntimeException(trailers);

        BusinessException restored = RpcErrors.toBusinessException(failed);

        assertThat(restored.errorCode().code()).isEqualTo("COMMON_404");
        assertThat(restored.getMessage()).isEqualTo("Order missing");
        assertThat(restored.errorCode().httpStatus()).isEqualTo(404);
    }

    @Test
    void fallsBackToTheStatusMappingWithoutATrailer() {
        BusinessException restored = RpcErrors.toBusinessException(
                Status.PERMISSION_DENIED.asRuntimeException());

        assertThat(restored.errorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
    }
}
