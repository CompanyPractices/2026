package com.processing.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for generated public OpenAPI documents
 */
@ConfigurationProperties(prefix = "gateway.open-api")
@Component
@Data
public class OpenApiProperties {
    /**
     * Creates empty OpenAPI properties for Spring configuration binding
     */
    public OpenApiProperties() {
    }

    private String url;
}
