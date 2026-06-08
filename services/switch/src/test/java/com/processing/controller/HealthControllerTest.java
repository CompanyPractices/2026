package com.processing.controller;

import com.processing.SwitchTestData;
import com.processing.config.SwitchProperties;
import com.processing.service.AuthorizationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HealthControllerTest {

    private MockRestServiceServer mockServer;
    private AuthorizationClient authorizationClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        authorizationClient = new AuthorizationClient(SwitchTestData.defaultProperties(), builder.build());
    }

    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    @Test
    void health_returnsSwitchStatusAndVersion() {
        mockServer.expect(requestTo("http://localhost:8083/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        SwitchProperties properties = SwitchTestData.defaultProperties();
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
