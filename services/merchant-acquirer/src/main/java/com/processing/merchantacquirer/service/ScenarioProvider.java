package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.domain.entity.ScenarioProperties;
import com.processing.merchantacquirer.domain.entity.ScenarioType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScenarioProvider {
    private final ScenarioProperties scenarioProperties;

    public Scenario getScenario(ScenarioType type){
        Scenario scenario = scenarioProperties.scenarios().get(type);
        return scenario;
    }
}
