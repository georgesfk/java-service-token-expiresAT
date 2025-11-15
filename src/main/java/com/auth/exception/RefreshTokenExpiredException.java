package com.auth.exception;

/**
 * Exception levée quand un refresh token a expiré
 */
public class RefreshTokenExpiredException extends RuntimeException {
    public RefreshTokenExpiredException(String message) {
        super(message);
    }

    public RefreshTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
