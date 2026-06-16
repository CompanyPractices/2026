package com.processing.merchantacquirer.client;

import com.processing.merchantacquirer.client.dto.CardsRequest;
import com.processing.merchantacquirer.client.dto.CardsResponse;
import com.processing.merchantacquirer.exception.ExternalServiceException;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Component
public class GatewayClient {
  private final RestClient restClient;

  public GatewayClient(RestClient.Builder builder, @Value("${gateway.url}") String gatewayUrl) {
      this.restClient = builder.baseUrl(gatewayUrl).build();
  }

  public CardsResponse getCards(CardsRequest request) {
    try {
      return restClient.get()
              .uri(uriBuilder -> {
                uriBuilder.path("/api/cards");
                if (request.limit() > 0) {
                    uriBuilder.queryParam("limit", request.limit());
                }
                if (request.offset() > 0) {
                    uriBuilder.queryParam("offset", request.offset());
                }
                if (request.status() != null) {
                  uriBuilder.queryParam("status", request.status());
                }
                if (request.bin() != null) {
                  uriBuilder.queryParam("bin", request.bin());
                }
                return uriBuilder.build();
              })
              .retrieve()
              .body(CardsResponse.class);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw ExternalServiceException.fromResponse(ex);
    } catch (Exception ex) {
      throw new ExternalServiceException("Card management", ex.getMessage(), "0");
    }
  }

  public AuthorizationResponse processAuthorize(AuthorizationRequest authorizationRequest) {
    try {
      return restClient
          .post()
              .uri("/api/transactions")
              .body(authorizationRequest)
              .retrieve()
              .body(AuthorizationResponse.class);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw ExternalServiceException.fromResponse(ex);
    } catch (Exception ex) {
      throw new ExternalServiceException("API Gateway", ex.getMessage(), "0");
    }
  }
}
