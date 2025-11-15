package com.auth.exception;

/**
 * Exception levée quand un refresh token est invalide ou introuvable
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
