package com.processing.kms.properties;

import com.processing.kms.models.ApiKeyRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "kms")
@Component
@Data
public class KmsProperties {
    private List<ApiClient> clients = new ArrayList<>();
    private Integer ttl = 60; // minutes
    private Generation generation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiClient {
        private String type;
        private ApiKeyRole role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Generation {
        private String algorithm = "AES";
        private Integer length = 256;
    }
}
