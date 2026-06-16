package com.processing.authorization.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.processing.common.utils.MaskPan;

/**
 * Конфигурация для Spring application
 */
@Configuration
public class AppConfig {
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
    @Bean
    public MaskPan maskPan() {
        return new MaskPan();
    }
}
