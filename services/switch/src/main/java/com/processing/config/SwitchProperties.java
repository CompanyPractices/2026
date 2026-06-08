package com.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "switch")
public record SwitchProperties(
        String version,
        Map<String, String> binRouting,
        String authorizationUrl,
        String loggerUrl,
        RetryProperties retry
) {
    public record RetryProperties(
            int maxAttempts,
            int loggerReadTimeoutMs
    ) {
        public static RetryProperties defaults() {
            return new RetryProperties(3, 2000);
        }
    }

    public RetryProperties retryOrDefaults() {
        return retry != null ? retry : RetryProperties.defaults();
    }
}
