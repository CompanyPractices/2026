package com.processing.merchantacquirer.controller.dto;

import java.util.Map;

public record HealthResponse(String status, String service, Map<String, String> dependencies) {}
