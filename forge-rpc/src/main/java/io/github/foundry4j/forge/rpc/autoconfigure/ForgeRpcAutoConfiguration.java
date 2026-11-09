package io.github.foundry4j.forge.rpc.autoconfigure;

import io.github.foundry4j.forge.rpc.RpcProperties;
import io.github.foundry4j.forge.rpc.client.RpcClientTraceInterceptor;
import io.github.foundry4j.forge.rpc.server.RpcServerExceptionInterceptor;
import io.github.foundry4j.forge.rpc.server.RpcServerTraceInterceptor;
import io.grpc.ServerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// Registers the framework gRPC interceptors.
///
/// Plain gRPC has no global interceptor registry, so the beans are exposed and picked up
/// by whatever server assembly the application uses.
@AutoConfiguration
@ConditionalOnClass(ServerInterceptor.class)
@EnableConfigurationProperties(RpcProperties.class)
public class ForgeRpcAutoConfiguration {

    /// Converts business exceptions into gRPC status codes.
    @Bean
    @ConditionalOnMissingBean
    public RpcServerExceptionInterceptor forgeRpcServerExceptionInterceptor() {
        return new RpcServerExceptionInterceptor();
    }

    /// Copies the incoming trace id into the logging MDC.
    @Bean
    @ConditionalOnMissingBean
    public RpcServerTraceInterceptor forgeRpcServerTraceInterceptor(RpcProperties properties) {
        return new RpcServerTraceInterceptor(properties.trace().enabled());
    }

    /// Sends the current trace id to downstream services.
    @Bean
    @ConditionalOnMissingBean
    public RpcClientTraceInterceptor forgeRpcClientTraceInterceptor(RpcProperties properties) {
        return new RpcClientTraceInterceptor(properties.trace().enabled());
    }
}
