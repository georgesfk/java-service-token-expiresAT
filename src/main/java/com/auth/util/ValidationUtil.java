package com.auth.util;

import org.springframework.stereotype.Component;

/**
 * Utilitaire pour valider les entrées utilisateur
 */
@Component
public class ValidationUtil {
    
    /**
     * Vérifie qu'une chaîne n'est pas null ou vide
     */
    public void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " ne peut pas être vide");
        }
    }

    /**
     * Vérifie plusieurs chaînes à la fois
     */
    public void validateNotEmpty(String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null || values[i].trim().isEmpty()) {
                throw new IllegalArgumentException("Le paramètre " + (i + 1) + " ne peut pas être vide");
            }
        }
    }

    /**
     * Vérifie que le username et password respectent les critères minimums
     */
    public void validateCredentials(String username, String password) {
        validateNotEmpty(username, "Username");
        validateNotEmpty(password, "Password");
        
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username doit contenir au moins 3 caractères");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password doit contenir au moins 6 caractères");
        }
    }
}
