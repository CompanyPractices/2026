package com.processing.config;

import io.github.resilience4j.retry.Retry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public Retry loggerRetry(SwitchProperties properties) {
        return RetryFactory.loggerRetry(properties);
    }

    @Bean
    public Retry authorizationRetry(SwitchProperties properties) {
        return RetryFactory.authorizationRetry(properties);
    }
}
