package com.processing.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "gateway.open-api")
@Component
@Data
public class OpenApiProperties {
    private String url;
}
