package com.processing.gateway.service;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DownstreamServiceResolver {
    public Optional<String> resolve(String path) {
        if ("/api/transactions".equals(path)) {
            return Optional.of("switch");
        }
        if (path.startsWith("/api/cards/") || "/api/cards".equals(path)) {
            return Optional.of("cardManagement");
        }
        if ("/api/transactions/search".equals(path) || path.startsWith("/api/dashboard/")) {
            return Optional.of("logger");
        }
        if (path.startsWith("/api/simulator/terminal/")) {
            return Optional.of("terminalSimulator");
        }
        if (path.startsWith("/api/simulator/merchant/")) {
            return Optional.of("merchantSimulator");
        }

        return Optional.empty();
    }
}
