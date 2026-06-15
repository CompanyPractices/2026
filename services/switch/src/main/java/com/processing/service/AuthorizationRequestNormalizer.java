package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;

final class AuthorizationRequestNormalizer {

    private static final int TERMINAL_ID_LENGTH = 8;
    private static final int MERCHANT_ID_LENGTH = 15;
    private static final String MERCHANT_PREFIX = "MERCH";

    private AuthorizationRequestNormalizer() {
    }

    static AuthorizationRequest normalize(AuthorizationRequest request) {
        return new AuthorizationRequest(
                request.mti(),
                request.stan(),
                request.pan(),
                request.processingCode(),
                request.amount(),
                request.currencyCode(),
                request.transmissionDateTime(),
                normalizeTerminalId(request.terminalId()),
                request.terminalType(),
                normalizeMerchantId(request.merchantId()),
                request.mcc(),
                request.acquirerId(),
                request.issuerId()
        );
    }

    static String normalizeTerminalId(String terminalId) {
        if (terminalId == null || terminalId.isBlank()) {
            return terminalId;
        }
        if (terminalId.length() >= TERMINAL_ID_LENGTH) {
            return terminalId.substring(0, TERMINAL_ID_LENGTH);
        }
        return padEnd(terminalId, TERMINAL_ID_LENGTH, '0');
    }

    static String normalizeMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return merchantId;
        }
        if (merchantId.length() == MERCHANT_ID_LENGTH) {
            return merchantId;
        }
        if (merchantId.length() == MERCHANT_ID_LENGTH + 1
                && merchantId.startsWith(MERCHANT_PREFIX)
                && merchantId.charAt(MERCHANT_PREFIX.length()) == '0') {
            return merchantId.substring(0, MERCHANT_PREFIX.length()) + merchantId.substring(MERCHANT_PREFIX.length() + 1);
        }
        if (merchantId.length() > MERCHANT_ID_LENGTH) {
            return merchantId.substring(0, MERCHANT_ID_LENGTH);
        }
        return padEnd(merchantId, MERCHANT_ID_LENGTH, '0');
    }

    private static String padEnd(String value, int length, char padChar) {
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < length) {
            builder.append(padChar);
        }
        return builder.toString();
    }
}
