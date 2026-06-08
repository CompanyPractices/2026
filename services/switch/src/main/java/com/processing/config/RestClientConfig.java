package com.processing.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int AUTH_READ_TIMEOUT_MS = 5000;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestFactory(requestFactory(CONNECT_TIMEOUT_MS, AUTH_READ_TIMEOUT_MS))
                .build();
    }

    @Bean
    @Qualifier("loggerRestClient")
    public RestClient loggerRestClient(SwitchProperties switchProperties) {
        return RestClient.builder()
                .requestFactory(requestFactory(CONNECT_TIMEOUT_MS, switchProperties.loggerReadTimeoutMs()))
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }
}
