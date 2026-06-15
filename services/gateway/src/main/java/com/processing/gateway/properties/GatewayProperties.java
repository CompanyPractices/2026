package com.processing.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Top-level gateway configuration properties.
 */
@ConfigurationProperties(prefix = "gateway")
@Component
@Data
public class GatewayProperties {
    private String version;
}
