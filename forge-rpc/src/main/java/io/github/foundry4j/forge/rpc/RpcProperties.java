package io.github.foundry4j.forge.rpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration of the RPC layer.
///
/// @param trace trace propagation settings
@ConfigurationProperties("forge.rpc")
public record RpcProperties(Trace trace) {

    /// Applies defaults.
    public RpcProperties {
        trace = trace == null ? new Trace(null) : trace;
    }

    /// Trace propagation settings.
    ///
    /// @param enabled whether trace ids travel through gRPC metadata, defaults to true
    public record Trace(Boolean enabled) {

        /// Applies the default.
        public Trace {
            enabled = enabled == null || enabled;
        }
    }
}
