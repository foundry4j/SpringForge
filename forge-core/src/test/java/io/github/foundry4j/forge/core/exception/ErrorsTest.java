package io.github.foundry4j.forge.core.exception;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the exception factory.
class ErrorsTest {

    @Test
    void buildsDomainExceptions() {
        var notFound = Errors.notFound("Order", 7L);
        assertThat(notFound.errorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
        assertThat(notFound.getMessage()).contains("Order").contains("7");

        assertThat(Errors.conflict("duplicate").errorCode()).isEqualTo(CommonErrorCode.CONFLICT);
        assertThat(Errors.badRequest("invalid").errorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST);
        assertThat(Errors.forbidden("denied").errorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
        assertThat(Errors.locked("order-1").errorCode()).isEqualTo(CommonErrorCode.LOCKED);
        assertThat(Errors.locked("order-1").getMessage()).contains("order-1");
    }

    @Test
    void enforcesMinimumRetryAfter() {
        assertThat(new TooManyRequestsException("slow down", 0).retryAfterSeconds()).isEqualTo(1);
        assertThat(new TooManyRequestsException("slow down", 42).retryAfterSeconds()).isEqualTo(42);
    }
}
