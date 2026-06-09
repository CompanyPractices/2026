package com.processing.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties with base URLs of downstream services
 */
@ConfigurationProperties(prefix = "gateway.downstream-services.urls")
@Component
@Data
public class ServiceProperties {
    /**
     * Creates empty service properties for Spring configuration binding
     */
    public ServiceProperties() {
    }

    private String switchUrl;
    private String loggerUrl;
    private String authUrl;
    private String cardsUrl;
}
