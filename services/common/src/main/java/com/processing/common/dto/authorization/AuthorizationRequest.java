package com.processing.common.dto.authorization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AuthorizationRequest(
        @Schema(description = "Request code", example = "0100")
        String mti,
        @Schema(description = "System trace audit number", example = "000000")
        String stan,
        @Schema(description = "Number card", example = "4000003458730237")
        String pan,
        @Schema(description = "Processing code", example = "000000")
        String processingCode,
        @Schema(description = "Amount", example = "72472")
        Integer amount,
        @Schema(description = "Currency type", example = "643")
        String currencyCode,
        @Schema(description = "Transaction time", example = "2026-06-05T18:12:49.07")
        String transmissionDateTime,
        @Schema(description = "Terminal id", example = "TERM001")
        String terminalId,
        @Schema(description = "Terminal type", example = "POS")
        String terminalType,
        @Schema(description = "Merchant ID", example = "MERCH00000000029")
        String merchantId,
        @Schema(description = "Merchant category code", example = "5045")
        String mcc,
        @Schema(description = "Acquirer ID", example = "ACQ002")
        String acquirerId
) {
}
