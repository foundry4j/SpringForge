package io.github.foundry4j.forge.core.exception;

import io.github.foundry4j.forge.core.error.CommonErrorCode;

/// Raised when a rate limit or quota has been exhausted.
public final class TooManyRequestsException extends BusinessException {

    private final long retryAfterSeconds;

    /// Creates a failure carrying the number of seconds the client should wait.
    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(CommonErrorCode.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = Math.max(retryAfterSeconds, 1L);
    }

    /// Seconds the client should wait before retrying.
    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
