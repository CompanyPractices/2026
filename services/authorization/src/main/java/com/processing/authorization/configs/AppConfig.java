package com.processing.authorization.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация для Spring application
 */
@Configuration
public class AppConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }
}
