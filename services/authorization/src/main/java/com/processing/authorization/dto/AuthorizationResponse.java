package com.processing.authorization.dto;

import lombok.Setter;
import lombok.Getter;
import lombok.AllArgsConstructor;
import com.processing.authorization.enums.AuthorizationRequestStatus;

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

    private Long processingTimeMs;

    public static AuthorizationResponse approved(AuthorizationRequest request, String rrn, String authCode) {
        return new AuthorizationResponse(
                request.getMti(),
                request.getStan(),
                rrn,
                authCode,
                "00",
                AuthorizationRequestStatus.APPROVED,
                null);
    }

    public static AuthorizationResponse declined(AuthorizationRequest request, String reason, String code) {
        return new AuthorizationResponse(
                request.getMti(),
                request.getStan(),
                null,
                null,
                code,
                AuthorizationRequestStatus.DECLINED,
                reason);
    }

    public AuthorizationResponse(String mti, String stan, String rrn, String authCode, String responceCode,
            AuthorizationRequestStatus status, String reason) {
        this.mti = mti;
        this.stan = stan;
        this.rrn = rrn;
        this.authCode = authCode;
        this.responseCode = responceCode;
        this.status = status;
        this.declineReason = reason;
    }

}
