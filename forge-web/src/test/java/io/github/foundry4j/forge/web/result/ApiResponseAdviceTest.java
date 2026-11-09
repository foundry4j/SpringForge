package io.github.foundry4j.forge.web.result;

import io.github.foundry4j.forge.core.result.ApiResult;
import io.github.foundry4j.forge.web.ForgeWebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that streaming bodies and resources bypass the response wrapper.
class ApiResponseAdviceTest {

    private final ApiResponseAdvice advice = new ApiResponseAdvice(
            new ForgeWebProperties(null, null, null, null, null));

    @Test
    void skipsStreamingBodies() {
        StreamingResponseBody body = out -> {
        };

        Object result = advice.beforeBodyWrite(body, method("download"), MediaType.APPLICATION_OCTET_STREAM,
                null, null, null);

        assertThat(result).isSameAs(body);
    }

    @Test
    void wrapsPlainPayloads() {
        Payload payload = new Payload("payload");

        Object result = advice.beforeBodyWrite(payload, method("download"), MediaType.APPLICATION_JSON,
                null, null, null);

        assertThat(result).isInstanceOf(ApiResult.class);
        assertThat(((ApiResult<?>) result).data()).isEqualTo(payload);
    }

    /// Simple payload used to verify wrapping.
    record Payload(String value) {
    }

    private static MethodParameter method(String name) {
        try {
            return new MethodParameter(SampleController.class.getMethod(name), -1);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /// Minimal controller used only to obtain a [MethodParameter].
    static class SampleController {

        /// Download endpoint.
        public StreamingResponseBody download() {
            return null;
        }
    }
}
