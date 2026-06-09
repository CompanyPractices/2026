package com.processing.common.dto.authorization;


import io.swagger.v3.oas.annotations.media.Schema;


public record AuthorizationResponse(
        @Schema(description = "Response code", example = "0110")
        String mti,
        @Schema(description = "System trace audit number", example = "000301")
        String stan,
        @Schema(description = "Retrieval reference number", example = "012345678901")
        String rrn,
        @Schema(description = "Code authorization", example = "TEST01")
        String authCode,
        @Schema(description = "Response code", example = "00")
        String responseCode,
        @Schema(description = "Status", example = "APPROVED")
        String status,
        @Schema(description = "Reason for refusal")
        String declineReason,
        @Schema(description = "Processing time", example = "67")
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
                        "System error", 0);
        }
}
