package com.processing.exception;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String error,
        String message,
        OffsetDateTime timestamp,
        String serviceName,
        int retryAfterMs
) {
    public static Builder builder(String error, String message) {
        return new Builder(error, message);
    }

    public static class Builder {
        private final String error;
        private final String message;
        private OffsetDateTime timestamp = OffsetDateTime.now();
        private String serviceName;
        private int retryAfterMs;

        private Builder(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public Builder timestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder retryAfterMs(int retryAfterMs) {
            this.retryAfterMs = retryAfterMs;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(error, message, timestamp, serviceName, retryAfterMs);
        }
    }
}
