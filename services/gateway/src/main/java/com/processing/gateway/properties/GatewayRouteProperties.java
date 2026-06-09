package com.processing.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway route properties bound from Spring Cloud Gateway MVC configuration
 */
@Component
@ConfigurationProperties(prefix = "spring.cloud.gateway.mvc")
@Data
public class GatewayRouteProperties {

    private List<RouteDefinition> routes = new ArrayList<>();

    /**
     * Creates empty route properties for Spring configuration binding
     */
    public GatewayRouteProperties() {
    }

    /**
     * Single gateway route definition used for routing and service-name resolution
     */
    @Data
    public static class RouteDefinition {
        private String id;
        private String uri;
        private Map<String, String> metadata = new HashMap<>();
        private List<String> predicates = new ArrayList<>();
        private List<String> filters = new ArrayList<>();

        /**
         * Creates an empty route definition for Spring configuration binding
         */
        public RouteDefinition() {
        }
    }
}
