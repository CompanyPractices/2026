package com.processing.merchantacquirer.client;

import com.processing.merchantacquirer.client.dto.CardsRequest;
import com.processing.merchantacquirer.client.dto.CardsResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GatewayClient {
    private final RestTemplate restTemplate;

    public GatewayClient(RestTemplateBuilder restTemplateBuilder){
        this.restTemplate = restTemplateBuilder.build();
    }

    public CardsResponse getCards(CardsRequest request){
        String url = "http://gateway:8080/api/cards";
        if(request.limit() > 0){
            url += "?limit=" + request.limit();
        }
        return restTemplate.getForEntity(url, CardsResponse.class).getBody();
    }
}
