package io.github.foundry4j.forge.notification;

/// Sends short messages through a provider specific implementation.
///
/// The framework ships a logging sender. An application replaces it by declaring a
/// `SmsSender` bean backed by the SDK of its provider:
///
/// ```java
/// @Bean
/// SmsSender aliyunSmsSender(AliyunClient client) {
///     return (phone, text) -> client.send(phone, text);
/// }
/// ```
@FunctionalInterface
public interface SmsSender {

    /// Sends one message.
    ///
    /// @param phone recipient number
    /// @param text  message body
    void send(String phone, String text);
}
