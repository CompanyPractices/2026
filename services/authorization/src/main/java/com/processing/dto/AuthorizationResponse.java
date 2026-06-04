package com.processing.dto;

import lombok.Setter;
import lombok.Getter;
import lombok.AllArgsConstructor;
import com.processing.enums.AuthorizationRequestStatus;

@Getter
@Setter
@AllArgsConstructor
public class AuthorizationResponse {
    private String mti;

    private String stan;

    private String rrn;

    private String authCode;

    private String responseCode;

    private AuthorizationRequestStatus status;

    private String declineReason;

    private Integer processingTimeMs;

    public static AuthorizationResponse approved(AuthorizationRequest request, String rrn, String authCode) {
        return new AuthorizationResponse(
            request.getMti(),
            request.getStan(),
            rrn,
            authCode,
            "00",
            AuthorizationRequestStatus.APPROVED,
            null,
            1 // TODO
        );
    }
 
    public static AuthorizationResponse declined(AuthorizationRequest request, String reason, String code) {
        return new AuthorizationResponse(
            request.getMti(),
            request.getStan(),
            null,
            null,
            code,
            AuthorizationRequestStatus.DECLINED,
            reason,
            1 // TODO
        );
    }

}
