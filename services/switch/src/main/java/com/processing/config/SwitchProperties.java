package com.processing.config;


import org.springframework.boot.context.properties.ConfigurationProperties;


import java.util.Map;


@ConfigurationProperties(prefix = "switch")
public record SwitchProperties(
        String version,
        Map<String, String> binRouting,
        String authorizationUrl,
        String loggerUrl,
        RetryProperties retry,
        int loggerReadTimeoutMs
) {
    public record RetryProperties(int maxAttempts) {}
}
