package com.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Implémentation de JwtUtil utilisant JJWT
 */
@Slf4j
@Component
public class JwtUtilImpl implements JwtUtil {
    
    @Value("${jwt.secret:my-secret-key-for-jwt-token-generation-minimum-256-bits-long}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:3600000}")  // 1 heure par défaut
    private long jwtExpiration;

    /**
     * Génère un access token JWT
     */
    @Override
    public String generateAccessToken(String username) {
        log.debug("Génération d'un access token pour: {}", username);
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        String token = Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "access")
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
        
        log.debug("Access token généré avec succès pour: {}", username);
        return token;
    }

    /**
     * Valide et extrait les claims d'un token
     */
    public Claims validateAndGetClaims(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            log.warn("Token invalide: {}", e.getMessage());
            throw new RuntimeException("Token invalide", e);
        }
    }

    /**
     * Extrait le username d'un token
     */
    public String getUsernameFromToken(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    /**
     * Vérifie si un token a expiré
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
