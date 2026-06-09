package com.processing.gateway.service;

import com.processing.gateway.properties.GatewayRouteProperties;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Optional;

/**
 * Resolves downstream service names from configured gateway route path predicates
 */
@Component
public class DownstreamServiceResolver {

    private static final String SERVICE_NAME_METADATA_KEY = "serviceName";
    private static final String PATH_PREDICATE_PREFIX = "Path=";

    private final List<RouteMapping> mappings;

    /**
     * Creates a resolver from Spring Cloud Gateway MVC route definitions
     *
     * @param properties gateway route properties
     */
    public DownstreamServiceResolver(GatewayRouteProperties properties) {
        PathPatternParser parser = new PathPatternParser();

        this.mappings = properties.getRoutes().stream()
                .flatMap(route -> toRouteMapping(route, parser).stream())
                .toList();
    }

    /**
     * Resolves the downstream service configured for a request path
     *
     * @param path request path
     * @return service name from route metadata, or empty if the path is not mapped
     */
    public Optional<String> resolve(String path) {
        PathContainer parsedPath = PathContainer.parsePath(path);

        return mappings.stream()
                .filter(mapping -> mapping.patterns().stream()
                        .anyMatch(pattern -> pattern.matches(parsedPath)))
                .map(RouteMapping::serviceName)
                .findFirst();
    }

    private Optional<RouteMapping> toRouteMapping(GatewayRouteProperties.RouteDefinition route,
                                                  PathPatternParser parser) {
        String serviceName = route.getMetadata().get(SERVICE_NAME_METADATA_KEY);
        if (serviceName == null || serviceName.isBlank()) {
            return Optional.empty();
        }

        List<PathPattern> patterns = route.getPredicates().stream()
                .filter(predicate -> predicate.startsWith(PATH_PREDICATE_PREFIX))
                .flatMap(predicate -> parsePathPatterns(predicate, parser).stream())
                .toList();

        if (patterns.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RouteMapping(serviceName, patterns));
    }

    private List<PathPattern> parsePathPatterns(String predicate, PathPatternParser parser) {
        String rawPatterns = predicate.substring(PATH_PREDICATE_PREFIX.length());

        return List.of(rawPatterns.split(",")).stream()
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .map(parser::parse)
                .toList();
    }

    private record RouteMapping(String serviceName, List<PathPattern> patterns) {
    }
}
