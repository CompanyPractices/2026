package com.processing.controller;

import com.processing.SwitchTestData;
import com.processing.config.SwitchProperties;
import com.processing.service.AuthorizationClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void health_returnsSwitchStatusAndVersion() {
        SwitchProperties properties = SwitchTestData.defaultProperties();
        AuthorizationClient authorizationClient = new AuthorizationClient(properties, null);
        HealthController controller = new HealthController(properties, authorizationClient);

        var response = controller.health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("ok");
        assertThat(response.getBody().service()).isEqualTo("switch");
        assertThat(response.getBody().version()).isEqualTo("1.0.0");
        assertThat(response.getBody().dependencies()).containsEntry("authorization", "ok");
    }
}
