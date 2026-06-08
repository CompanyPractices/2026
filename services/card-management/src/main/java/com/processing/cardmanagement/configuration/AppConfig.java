package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.services.LuhnValidator;
import com.processing.cardmanagement.services.PanGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Spring бинов приложения
 */
@Configuration
public class AppConfig {

    @Bean
    public PanGenerator panGenerator() {
        return new LuhnValidator();
    }
}
