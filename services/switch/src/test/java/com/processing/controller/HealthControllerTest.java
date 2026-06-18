package com.processing.controller;

import com.processing.SwitchTestData;
import com.processing.config.RetryFactory;
import com.processing.config.SwitchProperties;
import com.processing.service.AuthorizationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit-тесты {@link HealthController}.
 */
class HealthControllerTest {

    private HealthController controller;

    /** Настраивает mock Authorization health и контроллер. */
    @BeforeEach
    void setUp() {
        SwitchProperties properties = SwitchTestData.defaultProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://localhost:8083/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());
        AuthorizationClient authorizationClient = new AuthorizationClient(
                properties, builder.build(), RetryFactory.authorizationRetry(properties));
        controller = new HealthController(properties, authorizationClient);
    }

    /** Проверяет поля ответа {@code GET /health}. */
    @Test
    void health_returnsSwitchStatusAndVersion() {
        var response = controller.health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("ok");
        assertThat(response.getBody().service()).isEqualTo("switch");
        assertThat(response.getBody().version()).isEqualTo("1.0.0");
        assertThat(response.getBody().dependencies()).containsEntry("authorization", "ok");
    }
}
