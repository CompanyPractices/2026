package com.processing.gateway.openapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for rewriting aggregated OpenAPI server URLs.
 */
@ConfigurationProperties(prefix = "gateway.open-api")
@Component
@Data
public class OpenApiProperties {
    private String url;
}
