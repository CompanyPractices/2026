package com.processing.config;

import io.github.resilience4j.retry.Retry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring-конфигурация бинов Resilience4j Retry для Authorization и Logger.
 */
@Configuration
public class Resilience4jConfig {

    /**
     * @param properties конфигурация Switch
     * @return retry для Transaction Logger
     */
    @Bean
    public Retry loggerRetry(SwitchProperties properties) {
        return RetryFactory.loggerRetry(properties);
    }

    /**
     * @param properties конфигурация Switch
     * @return retry для Authorization Service
     */
    @Bean
    public Retry authorizationRetry(SwitchProperties properties) {
        return RetryFactory.authorizationRetry(properties);
    }
}
