package com.processing.merchantacquirer.domain.entity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@ConfigurationProperties(prefix = "simulation")
public record ScenarioProperties(
        Map<ScenarioType, Scenario> scenarios
) {
}
