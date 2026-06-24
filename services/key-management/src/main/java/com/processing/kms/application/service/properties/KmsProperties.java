package com.processing.kms.application.service.properties;

import com.processing.kms.domain.models.ApiKeyRole;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "kms")
@Component
@Data
public class KmsProperties {
    private List<ApiClient> clients = new ArrayList<>();
    private Integer ttlMin = 60; // minutes
    private Generation generation;

    @Data
    public static class ApiClient {
        private String type;
        private ApiKeyRole role;
    }

    @Data
    public static class Generation {
        private String algorithm = "AES";
        private Integer length = 256;
    }
}
