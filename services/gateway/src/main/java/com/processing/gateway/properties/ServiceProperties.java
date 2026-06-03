package com.processing.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "gateway.downstream-services.urls")
@Component
@Data
public class ServiceProperties {
    private String switchUrl;
    private String loggerUrl;
    private String authUrl;
    private String cardsUrl;
}
