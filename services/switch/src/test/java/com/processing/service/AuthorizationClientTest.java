package com.processing.service;

import com.processing.SwitchTestData;
import com.processing.config.SwitchProperties;
import com.processing.model.AuthorizationRequest;
import com.processing.model.AuthorizationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationClientTest {

    @Test
    void authorize_whenStubEnabled_returnsApproved() {
        AuthorizationClient client = new AuthorizationClient(SwitchTestData.defaultProperties(), null);

        AuthorizationRequest request = SwitchTestData.sampleRequest().withIssuerId("ISS001");
        AuthorizationResponse response = client.authorize(request);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.responseCode()).isEqualTo("00");
        assertThat(response.stan()).isEqualTo("000001");
        assertThat(response.authCode()).isEqualTo("TEST01");
    }

    @Test
    void checkHealth_whenStubEnabled_returnsOk() {
        AuthorizationClient client = new AuthorizationClient(SwitchTestData.defaultProperties(), null);

        assertThat(client.checkHealth()).isEqualTo("ok");
    }

    @Test
    void authorize_whenStubDisabledAndAuthUnreachable_returnsDeclined05() {
        SwitchProperties properties = new SwitchProperties(
                "1.0.0",
                SwitchTestData.BIN_ROUTING,
                "http://127.0.0.1:1",
                "http://127.0.0.1:1",
                false
        );
        AuthorizationClient client = new AuthorizationClient(properties, RestClient.create());

        AuthorizationResponse response = client.authorize(
                SwitchTestData.sampleRequest().withIssuerId("ISS001"));

        assertThat(response.responseCode()).isEqualTo("05");
        assertThat(response.status()).isEqualTo("DECLINED");
    }
}
