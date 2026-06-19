package com.processing.gateway.downstream;

import com.processing.gateway.common.properties.GatewayRouteProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DownstreamServiceResolverTest {

    @Test
    void shouldResolveServiceNameByPathPredicate() {
        DownstreamServiceResolver resolver = new DownstreamServiceResolver(routeProperties(
                route("switch", "Path=/api/transactions"),
                route("cardManagement", "Path=/api/cards/**")
        ));

        assertThat(resolver.resolve("/api/transactions")).contains("switch");
        assertThat(resolver.resolve("/api/cards/123")).contains("cardManagement");
    }

    @Test
    void shouldSupportCommaSeparatedPathPredicates() {
        DownstreamServiceResolver resolver = new DownstreamServiceResolver(routeProperties(
                route("authorization", "Path=/api/auth,/api/reversal")
        ));

        assertThat(resolver.resolve("/api/auth")).contains("authorization");
        assertThat(resolver.resolve("/api/reversal")).contains("authorization");
    }

    @Test
    void shouldIgnoreRoutesWithoutServiceNameOrPathPredicate() {
        GatewayRouteProperties.RouteDefinition noServiceName = route(null, "Path=/api/transactions");
        GatewayRouteProperties.RouteDefinition noPath = route("switch", "Method=POST");
        DownstreamServiceResolver resolver = new DownstreamServiceResolver(routeProperties(noServiceName, noPath));

        assertThat(resolver.resolve("/api/transactions")).isEqualTo(Optional.empty());
    }

    private GatewayRouteProperties routeProperties(GatewayRouteProperties.RouteDefinition... routes) {
        GatewayRouteProperties properties = new GatewayRouteProperties();
        properties.setRoutes(List.of(routes));
        return properties;
    }

    private GatewayRouteProperties.RouteDefinition route(String serviceName, String predicate) {
        GatewayRouteProperties.RouteDefinition route = new GatewayRouteProperties.RouteDefinition();
        if (serviceName != null) {
            route.setMetadata(Map.of("serviceName", serviceName));
        }
        route.setPredicates(List.of(predicate));
        return route;
    }
}
