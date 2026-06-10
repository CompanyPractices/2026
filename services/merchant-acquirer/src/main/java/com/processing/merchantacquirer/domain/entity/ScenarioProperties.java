package com.processing.merchantacquirer.domain.entity;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulation")
public record ScenarioProperties(Map<ScenarioType, Scenario> scenarios) {}
