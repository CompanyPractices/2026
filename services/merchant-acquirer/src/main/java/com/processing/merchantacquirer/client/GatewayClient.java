package com.processing.merchantacquirer.client;

import com.processing.merchantacquirer.client.dto.CardsRequest;
import com.processing.merchantacquirer.client.dto.CardsResponse;
import com.processing.merchantacquirer.domain.model.AuthorizationRequest;
import com.processing.merchantacquirer.domain.model.AuthorizationResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GatewayClient {
    private final RestTemplate restTemplate = new RestTemplate();

    public CardsResponse getCards(CardsRequest request){
        String url = "http://gateway:8080/api/cards";
        if(request.limit() > 0){
            url += "?limit=" + request.limit();
        }
        return restTemplate.getForEntity(url, CardsResponse.class).getBody();
    }

    public AuthorizationResponse processAuthorize(AuthorizationRequest authorizationRequest){
        String url = "http://gateway:8080/api/transactions";
        return restTemplate.postForEntity(url, authorizationRequest, AuthorizationResponse.class).getBody();
    }
}
