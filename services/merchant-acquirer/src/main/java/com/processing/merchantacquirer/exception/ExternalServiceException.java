package com.processing.merchantacquirer.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import com.processing.common.dto.ErrorResponse;
import org.springframework.web.client.HttpStatusCodeException;

public class ExternalServiceException extends RuntimeException{
    @Getter
    private final String serviceName;
    @Getter
    private final String retryAfterMs;

    public ExternalServiceException(String serviceName, String message, String retryAfterMs){
        super(message);
        this.serviceName = serviceName;
        this.retryAfterMs = retryAfterMs;
    }

    public static ExternalServiceException fromResponse(HttpStatusCodeException ex){
        try{
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse response = mapper.readValue(ex.getResponseBodyAsString(), ErrorResponse.class);
            return new ExternalServiceException(
                    response.serviceName(),
                    response.message(),
                    response.retryAfterMs()
            );
        } catch (JsonProcessingException e) {
            throw new ExternalServiceException(
                    "Merchant acquirer simulator",
                    ex.getMessage(),
                    "0"
            );
        }
    }
}
