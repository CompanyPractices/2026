package com.processing.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean
    @Qualifier("loggerRestClient")
    public RestClient loggerRestClient(SwitchProperties switchProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(switchProperties.retryOrDefaults().loggerReadTimeoutMs());
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
