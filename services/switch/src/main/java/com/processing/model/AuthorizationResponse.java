package com.processing.model;

public record AuthorizationResponse(
        String mti,
        String stan,
        String rrn,
        String authCode,
        String responseCode,
        String status,
        String declineReason,
        Integer processingTimeMs
) {
    public static AuthorizationResponse unknownBin(String stan) {
        return new AuthorizationResponse(
                "0110", stan, null, null, "14", "DECLINED",
                "Invalid card number (unknown BIN)", 0);
    }

    public static AuthorizationResponse authUnavailable(String stan) {
        return new AuthorizationResponse(
                "0110", stan, null, null, "05", "DECLINED",
                "Authorization service unavailable", 0);
    }

    public static AuthorizationResponse systemError(String stan) {
        return new AuthorizationResponse(
                "0110", stan, null, null, "96", "DECLINED",
                "System error (logger unavailable)", 0);
    }
}
