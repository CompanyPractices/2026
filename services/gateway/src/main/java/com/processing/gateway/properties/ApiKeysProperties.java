package com.processing.gateway.properties;

import com.processing.gateway.models.ApiKeyRoles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "gateway.api-keys")
@Component
@Data
public class ApiKeysProperties {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rules {
        private String url;
        private String method;
        private ApiKeyRoles role;
    }

    private List<Rules> rules = new ArrayList<>();
    private List<Rules> exclusions = new ArrayList<>();
    private Map<String, String> keys;
}
