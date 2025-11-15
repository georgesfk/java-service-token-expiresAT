package com.auth.util;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Utilitaire pour implémenter le rate limiting et la protection brute-force
 */
@Component
public class RateLimitingUtil {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    
    private final ConcurrentHashMap<String, AttemptInfo> attemptTracker = new ConcurrentHashMap<>();

    /**
     * Vérifie si un utilisateur a dépassé le nombre de tentatives
     */
    public void checkAttempt(String username) {
        AttemptInfo info = attemptTracker.compute(username, (key, existingInfo) -> {
            if (existingInfo == null) {
                return new AttemptInfo();
            }
            
            // Si la lockout a expiré, réinitialiser
            if (System.currentTimeMillis() - existingInfo.lockoutTime > 
                TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION_MINUTES)) {
                return new AttemptInfo();
            }
            
            return existingInfo;
        });

        // Vérifier si l'utilisateur est actuellement bloqué
        if (info.isLocked()) {
            long remainingSeconds = (LOCKOUT_DURATION_MINUTES * 60) - 
                ((System.currentTimeMillis() - info.lockoutTime) / 1000);
            throw new com.auth.exception.TooManyAuthAttemptsException(
                "Trop de tentatives. Réessayez dans " + remainingSeconds + " secondes",
                remainingSeconds
            );
        }
    }

    /**
     * Enregistre une tentative échouée
     */
    public void recordFailedAttempt(String username) {
        attemptTracker.compute(username, (key, info) -> {
            if (info == null) {
                info = new AttemptInfo();
            }
            
            // Si lockout expiré, réinitialiser
            if (System.currentTimeMillis() - info.lockoutTime > 
                TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION_MINUTES)) {
                info = new AttemptInfo();
            }
            
            info.failedAttempts++;
            if (info.failedAttempts >= MAX_ATTEMPTS) {
                info.lockoutTime = System.currentTimeMillis();
            }
            return info;
        });
    }

    /**
     * Réinitialise les tentatives pour un utilisateur (après succès)
     */
    public void resetAttempts(String username) {
        attemptTracker.remove(username);
    }

    /**
     * Classe interne pour tracker les tentatives
     */
    private static class AttemptInfo {
        int failedAttempts = 0;
        long lockoutTime = 0;

        boolean isLocked() {
            return failedAttempts >= MAX_ATTEMPTS;
        }
    }
}
