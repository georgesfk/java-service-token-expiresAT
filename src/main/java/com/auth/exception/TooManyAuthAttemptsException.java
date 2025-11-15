package com.auth.exception;

/**
 * Exception levée quand trop de tentatives d'authentification ont échoué (rate limiting)
 */
public class TooManyAuthAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyAuthAttemptsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
