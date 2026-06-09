package com.processing.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "gateway.health")
@Component
@Data
public class HealthProperties {
    private Integer connectionTimeout = 10;
    private Integer requestTimeout = 3;
}
