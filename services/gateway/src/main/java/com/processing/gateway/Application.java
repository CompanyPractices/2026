package com.processing.gateway;

import com.processing.gateway.properties.GatewayProperties;
import com.processing.gateway.properties.OpenApiProperties;
import com.processing.gateway.properties.ServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Spring Boot entry point for Gateway Service.
 */
@SpringBootApplication
@EnableConfigurationProperties({ServiceProperties.class, OpenApiProperties.class, GatewayProperties.class})
@EnableCaching
public class Application {

    /**
     * Starts the gateway application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
