package com.auth.service;

import com.auth.exception.InvalidRefreshTokenException;
import com.auth.exception.RefreshTokenExpiredException;
import com.auth.model.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service pour gérer les refresh tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Crée un nouveau refresh token pour un utilisateur
     */
    public RefreshToken createRefreshToken(String username) {
        log.debug("Création d'un refresh token pour l'utilisateur: {}", username);
        
        RefreshToken rt = new RefreshToken();
        rt.setToken(java.util.UUID.randomUUID().toString());
        rt.setUsername(username);
        rt.setExpiresAt(Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
        rt.setCreatedAt(Instant.now());
        rt.setRevoked(false);
        
        return refreshTokenRepository.save(rt);
    }

    /**
     * Récupère et valide un refresh token
     */
    public RefreshToken getValidRefreshToken(String token) {
        log.debug("Validation du refresh token");
        
        RefreshToken rt = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> {
                log.warn("Tentative avec un refresh token invalide");
                return new InvalidRefreshTokenException("Refresh token introuvable");
            });

        // Vérifier si le token a été révoqué
        if (rt.isRevoked()) {
            log.warn("Tentative d'utilisation d'un token révoqué pour: {}", rt.getUsername());
            throw new InvalidRefreshTokenException("Refresh token a été révoqué");
        }

        // Vérifier l'expiration
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            log.info("Token expiré pour l'utilisateur: {}", rt.getUsername());
            refreshTokenRepository.delete(rt);
            throw new RefreshTokenExpiredException("Refresh token a expiré");
        }

        return rt;
    }

    /**
     * Effectue la rotation du refresh token (ancien supprimé, nouveau créé)
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        log.info("Rotation du refresh token pour l'utilisateur: {}", oldToken.getUsername());
        
        // Supprimer l'ancien token
        refreshTokenRepository.delete(oldToken);
        
        // Créer un nouveau token
        return createRefreshToken(oldToken.getUsername());
    }

    /**
     * Révoque tous les tokens d'un utilisateur (logout complet)
     */
    @Transactional
    public void revokeAllUserTokens(String username) {
        log.info("Révocation de tous les tokens pour l'utilisateur: {}", username);
        refreshTokenRepository.revokeAllForUser(username);
    }

    /**
     * Révoque un token spécifique
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.info("Token révoqué pour l'utilisateur: {}", rt.getUsername());
        });
    }

    /**
     * Nettoie les tokens expirés de la base de données
     * Exécuté tous les jours à 2h du matin
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Début du nettoyage des tokens expirés");
        long deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Tokens expirés supprimés: {}", deletedCount);
    }
}
