package com.processing.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientFactory {

    @Bean
    public RestClient restClient(SwitchProperties switchProperties) {
        SwitchProperties.HttpProperties http = switchProperties.http();
        return RestClient.builder()
                .requestFactory(requestFactory(http.connectTimeoutMs(), http.authReadTimeoutMs()))
                .build();
    }

    @Bean
    @Qualifier("loggerRestClient")
    public RestClient loggerRestClient(SwitchProperties switchProperties) {
        SwitchProperties.HttpProperties http = switchProperties.http();
        return RestClient.builder()
                .requestFactory(requestFactory(http.connectTimeoutMs(), http.loggerReadTimeoutMs()))
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }
}
