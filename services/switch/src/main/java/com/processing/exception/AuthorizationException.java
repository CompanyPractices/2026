package com.processing.exception;

public class AuthorizationException extends RuntimeException {

    private final String stan;

    public AuthorizationException(String stan, int attempts, String lastError) {
        super(String.format(
                "Authorization service unavailable for STAN=%s after %d attempts. Last error: %s",
                stan, attempts, lastError != null ? lastError : "unknown"));
        this.stan = stan;
    }

    public String getStan() {
        return stan;
    }

}
