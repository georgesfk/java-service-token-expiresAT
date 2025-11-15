package com.auth.service;

import com.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

/**
 * Repository pour les RefreshTokens
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    /**
     * Trouve un token par sa valeur
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Trouve tous les tokens d'un utilisateur
     */
    List<RefreshToken> findByUsername(String username);
    
    /**
     * Supprime un token par sa valeur
     */
    void deleteByToken(String token);
    
    /**
     * Supprime tous les tokens d'un utilisateur
     */
    void deleteByUsername(String username);
    
    /**
     * Revoque tous les tokens d'un utilisateur (sans les supprimer)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.username = :username")
    void revokeAllForUser(@Param("username") String username, @Param("revokedAt") Instant revokedAt);
    
    /**
     * Revoque tous les tokens d'un utilisateur (version simplifiée)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.username = :username")
    void revokeAllForUser(@Param("username") String username);
    
    /**
     * Supprime les tokens expirés
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    long deleteExpiredTokens(@Param("now") Instant now);
    
    /**
     * Trouve les tokens qui expirent dans les N prochains jours (pour alertes)
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt BETWEEN :now AND :inNDays AND rt.revoked = false")
    List<RefreshToken> findTokensExpiringInNDays(
        @Param("now") Instant now,
        @Param("inNDays") Instant inNDays
    );
}
