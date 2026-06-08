package com.processing.merchantacquirer.client;

import com.processing.merchantacquirer.client.dto.CardsRequest;
import com.processing.merchantacquirer.client.dto.CardsResponse;
import com.processing.merchantacquirer.domain.model.AuthorizationRequest;
import com.processing.merchantacquirer.domain.model.AuthorizationResponse;
import com.processing.merchantacquirer.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GatewayClient {
  private final RestTemplate restTemplate = new RestTemplate();
  public final String gatewayUrl = "http://gateway:8080";

  public CardsResponse getCards(CardsRequest request) {
    String url = gatewayUrl + "/api/cards";
    if (request.limit() > 0) {
      url += "?limit=" + request.limit();
    }
    try {
      return restTemplate.getForEntity(url, CardsResponse.class).getBody();
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw ExternalServiceException.fromResponse(ex);
    } catch (Exception ex) {
      throw new ExternalServiceException("Card management", ex.getMessage(), "0");
    }
  }

  public AuthorizationResponse processAuthorize(AuthorizationRequest authorizationRequest) {
    String url = gatewayUrl + "/api/transactions";
    try {
      return restTemplate
          .postForEntity(url, authorizationRequest, AuthorizationResponse.class)
          .getBody();
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw ExternalServiceException.fromResponse(ex);
    } catch (Exception ex) {
      throw new ExternalServiceException("API Gateway", ex.getMessage(), "0");
    }
  }
}
