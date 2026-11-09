package io.github.foundry4j.forge.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;

/// Configuration of the web layer.
///
/// @param responseWrapping  when `true`, successful controller responses are wrapped into
///                         [io.github.foundry4j.forge.core.result.ApiResult]
/// @param problemTypePrefix prefix used to build the `type` URI of problem details
/// @param accessLog         access log settings
/// @param apiVersion        API version negotiation settings
/// @param file              uploaded file storage settings
@ConfigurationProperties("forge.web")
public record ForgeWebProperties(
        Boolean responseWrapping,
        String problemTypePrefix,
        AccessLog accessLog,
        ApiVersion apiVersion,
        File file
) {

    /// Applies defaults that make the framework productive out of the box.
    public ForgeWebProperties {
        responseWrapping = responseWrapping == null || responseWrapping;
        problemTypePrefix = problemTypePrefix == null || problemTypePrefix.isBlank()
                ? "urn:forge:error:"
                : problemTypePrefix;
        accessLog = accessLog == null ? new AccessLog(null) : accessLog;
        apiVersion = apiVersion == null ? new ApiVersion(null, null, null) : apiVersion;
        file = file == null ? new File(null, null, null) : file;
    }

    /// Access log settings.
    ///
    /// @param enabled whether one line per request is written to the log
    public record AccessLog(Boolean enabled) {

        /// Access logging is enabled by default.
        public AccessLog {
            enabled = enabled == null || enabled;
        }
    }

    /// API version negotiation settings.
    ///
    /// @param enabled        whether `@ApiVersion` endpoints are checked
    /// @param defaultVersion version assumed when the caller does not send one
    /// @param header         request and response header carrying the version
    public record ApiVersion(Boolean enabled, String defaultVersion, String header) {

        /// Applies the defaults `v1` and `X-API-Version`.
        public ApiVersion {
            enabled = enabled == null || enabled;
            defaultVersion = defaultVersion == null || defaultVersion.isBlank() ? "v1" : defaultVersion;
            header = header == null || header.isBlank() ? "X-API-Version" : header;
        }
    }

    /// Local file storage settings.
    ///
    /// @param baseDir             root directory of the stored files
    /// @param maxSize             maximum accepted size of a single file
    /// @param allowedContentTypes accepted MIME types, empty means no restriction
    public record File(String baseDir, DataSize maxSize, List<String> allowedContentTypes) {

        /// Defaults to a directory below the temporary directory and a 10 MB limit.
        public File {
            baseDir = baseDir == null || baseDir.isBlank()
                    ? System.getProperty("java.io.tmpdir") + "/forge-files"
                    : baseDir;
            maxSize = maxSize == null ? DataSize.ofMegabytes(10) : maxSize;
            allowedContentTypes = allowedContentTypes == null ? List.of() : List.copyOf(allowedContentTypes);
        }
    }
}
