package com.processing.config;


import org.springframework.boot.context.properties.ConfigurationProperties;


import java.util.List;
import java.util.Map;


@ConfigurationProperties(prefix = "switch")
public record SwitchProperties(
        String version,
        Map<String, String> binRouting,
        String authorizationUrl,
        String loggerUrl,
        String merchantAcquirerUrl,
        HttpProperties http,
        RetryProperties retry
) {
    public record HttpProperties(
            int connectTimeoutMs,
            int authReadTimeoutMs,
            int loggerReadTimeoutMs
    ) {}

    public record RetryProperties(
            int maxAttempts,
            List<Long> backoffMs
    ) {}
}
