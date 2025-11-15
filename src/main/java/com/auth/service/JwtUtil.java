package com.auth.service;

/**
 * Interface stub pour JwtUtil - À implémenter selon votre configuration JWT
 */
public interface JwtUtil {
    /**
     * Génère un access token JWT pour un utilisateur
     */
    String generateAccessToken(String username);
}
