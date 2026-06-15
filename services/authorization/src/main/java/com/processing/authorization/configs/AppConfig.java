package com.processing.authorization.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Конфигурация для Spring application
 */
@Configuration
public class AppConfig {
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
