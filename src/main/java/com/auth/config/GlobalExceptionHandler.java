package com.auth.config;

import com.auth.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global pour l'API
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les exceptions de token invalide
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<?> handleInvalidRefreshToken(
        InvalidRefreshTokenException ex, WebRequest request) {
        log.error("InvalidRefreshTokenException: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Gère les exceptions de token expiré
     */
    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<?> handleRefreshTokenExpired(
        RefreshTokenExpiredException ex, WebRequest request) {
        log.error("RefreshTokenExpiredException: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Gère les exceptions d'identifiants invalides
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(
        InvalidCredentialsException ex, WebRequest request) {
        log.warn("InvalidCredentialsException: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
    }

    /**
     * Gère les exceptions de rate limiting
     */
    @ExceptionHandler(TooManyAuthAttemptsException.class)
    public ResponseEntity<?> handleTooManyAttempts(
        TooManyAuthAttemptsException ex, WebRequest request) {
        log.warn("TooManyAuthAttemptsException: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Trop de tentatives");
        body.put("message", ex.getMessage());
        body.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        return new ResponseEntity<>(body, HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Gère les exceptions de validation
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(
        IllegalArgumentException ex, WebRequest request) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Gère les exceptions générales
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(
        Exception ex, WebRequest request) {
        log.error("Erreur non gérée", ex);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur serveur est survenue");
    }

    /**
     * Utilitaire pour créer une réponse d'erreur standardisée
     */
    private ResponseEntity<?> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
