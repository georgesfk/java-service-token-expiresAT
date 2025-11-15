package com.auth.service;

import com.auth.exception.InvalidCredentialsException;
import com.auth.exception.InvalidRefreshTokenException;
import com.auth.exception.RefreshTokenExpiredException;
import com.auth.exception.TooManyAuthAttemptsException;
import com.auth.model.RefreshToken;
import com.auth.util.RateLimitingUtil;
import com.auth.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'authentification amélioré avec sécurité et logging
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitingUtil rateLimitingUtil;
    private final ValidationUtil validationUtil;

    /**
     * Authentifie un utilisateur et crée des tokens d'accès et de rafraîchissement
     */
    @Transactional
    public AuthResponse login(String username, String password) {
        log.info("Tentative de connexion pour l'utilisateur: {}", username);
        
        // Validation des entrées
        try {
            validationUtil.validateCredentials(username, password);
        } catch (IllegalArgumentException e) {
            log.warn("Validation échouée pour {}: {}", username, e.getMessage());
            throw e;
        }

        // Vérifier le rate limiting
        try {
            rateLimitingUtil.checkAttempt(username);
        } catch (TooManyAuthAttemptsException e) {
            log.warn("Trop de tentatives pour {}", username);
            throw e;
        }

        // Authentifier l'utilisateur
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            log.info("Authentification réussie pour: {}", username);
        } catch (BadCredentialsException e) {
            rateLimitingUtil.recordFailedAttempt(username);
            log.warn("Identifiants invalides pour: {}", username);
            throw new InvalidCredentialsException("Identifiants invalides", e);
        } catch (AuthenticationException e) {
            rateLimitingUtil.recordFailedAttempt(username);
            log.error("Erreur d'authentification pour {}: {}", username, e.getMessage());
            throw new InvalidCredentialsException("Erreur d'authentification", e);
        }

        // Réinitialiser les tentatives échouées
        rateLimitingUtil.resetAttempts(username);

        // Générer les tokens
        String accessToken = jwtUtil.generateAccessToken(username);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);

        log.info("Connexion réussie pour: {}", username);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    /**
     * Rafraîchit les tokens en utilisant un refresh token valide
     */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        log.info("Rafraîchissement du token");
        
        // Validation
        validationUtil.validateNotEmpty(refreshToken, "Refresh token");

        try {
            // Valider le refresh token
            RefreshToken rt = refreshTokenService.getValidRefreshToken(refreshToken);

            // Générer un nouvel access token
            String newAccessToken = jwtUtil.generateAccessToken(rt.getUsername());

            // Effectuer la rotation du refresh token
            RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(rt);

            log.info("Token rafraîchi avec succès pour: {}", rt.getUsername());
            return new AuthResponse(newAccessToken, newRefreshToken.getToken());

        } catch (InvalidRefreshTokenException | RefreshTokenExpiredException e) {
            log.warn("Erreur lors du rafraîchissement: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erreur inattendue lors du rafraîchissement", e);
            throw new RuntimeException("Erreur lors du rafraîchissement du token", e);
        }
    }

    /**
     * Déconnecte un utilisateur en révoquant son refresh token
     */
    @Transactional
    public void logout(String refreshToken) {
        log.info("Logout en cours");
        
        // Validation
        validationUtil.validateNotEmpty(refreshToken, "Refresh token");

        try {
            refreshTokenService.revokeToken(refreshToken);
            log.info("Logout réussi");
        } catch (Exception e) {
            log.error("Erreur lors du logout", e);
            throw new RuntimeException("Erreur lors du logout", e);
        }
    }

    /**
     * Révoque tous les tokens d'un utilisateur (déconnexion de tous les appareils)
     */
    @Transactional
    public void logoutAll(String username) {
        log.info("Logout complet pour l'utilisateur: {}", username);
        
        validationUtil.validateNotEmpty(username, "Username");

        try {
            refreshTokenService.revokeAllUserTokens(username);
            log.info("Logout complet réussi pour: {}", username);
        } catch (Exception e) {
            log.error("Erreur lors du logout complet", e);
            throw new RuntimeException("Erreur lors du logout complet", e);
        }
    }
}
