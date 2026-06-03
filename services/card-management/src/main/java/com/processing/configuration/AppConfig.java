package com.processing.configuration;

import com.processing.services.LuhnValidator;
import com.processing.services.PanGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public PanGenerator panGenerator() {
        return new LuhnValidator();
    }
}
