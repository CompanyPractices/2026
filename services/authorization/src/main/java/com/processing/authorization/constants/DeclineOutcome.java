package com.processing.authorization.constants;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackResponse;

import java.time.Instant;
import java.time.LocalDateTime;

public record DeclineOutcome(String reason, String code) {
    public static final DeclineOutcome CARD_NOT_FOUND = new DeclineOutcome(
            "CARD_NOT_FOUND",
            AuthorizationResponse.CODE_CARD_NOT_FOUND
    );

    public static final DeclineOutcome SERVICE_UNAVAILABLE = new DeclineOutcome(
            "SERVICE_UNAVAILABLE",
            AuthorizationResponse.CODE_SERVICE_UNAVAILABLE
    );

    public static final DeclineOutcome UNKNOWN_REASON = new DeclineOutcome(
            "UNKNOWN_REASON",
            AuthorizationResponse.CODE_DECLINED_GENERAL
    );

    public static final DeclineOutcome CARD_EXPIRED = new DeclineOutcome(
            "CARD_EXPIRED",
            AuthorizationResponse.CODE_CARD_EXPIRED
    );

    public static final DeclineOutcome CARD_BLOCKED = new DeclineOutcome(
            "CARD_BLOCKED",
            AuthorizationResponse.CODE_DECLINED_GENERAL
    );

    public static final DeclineOutcome CARD_INACTIVE = new DeclineOutcome(
            "CARD_INACTIVE",
            AuthorizationResponse.CODE_DECLINED_GENERAL
    );

    public static final DeclineOutcome EXCEEDS_AMOUNT_LIMIT = new DeclineOutcome(
            "EXCEEDS_AMOUNT_LIMIT",
            AuthorizationResponse.CODE_EXCEEDS_LIMIT
    );

    public static final DeclineOutcome INSUFFICIENT_FUNDS = new DeclineOutcome(
            "INSUFFICIENT_FUNDS",
            AuthorizationResponse.CODE_INSUFFICIENT_FUNDS
    );

    public static final DeclineOutcome RESERVATION_FAILED = new DeclineOutcome(
            "RESERVATION_FAILED",
            AuthorizationResponse.CODE_SERVICE_UNAVAILABLE
    );

    public static final DeclineOutcome TRANSACTION_NOT_FOUND = new DeclineOutcome(
            "TRANSACTION_NOT_FOUND",
            AuthorizationResponse.CODE_DECLINED_GENERAL
    );

    public static final DeclineOutcome ALREADY_ROLLED_BACK = new DeclineOutcome(
            "ALREADY_ROLLED_BACK",
            AuthorizationResponse.CODE_DECLINED_GENERAL
    );

    public static final DeclineOutcome ROLLBACK_FAILED = new DeclineOutcome(
            "ROLLBACK_FAILED",
            AuthorizationResponse.CODE_SERVICE_UNAVAILABLE
    );

    public AuthorizationResponse buildAuthorization(AuthorizationRequest request, LocalDateTime requestInputTime) {
        return AuthorizationResponse.declined(request, reason, code, requestInputTime);
    }

    public RollbackResponse buildRollback(String rrn, Instant requestInputTime) {
        return RollbackResponse.declined(rrn, reason, requestInputTime);
    }
}
