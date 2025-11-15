package com.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuration CORS pour les requêtes cross-origin
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origines autorisées
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080",
            "http://localhost:8081"
            // Ajouter d'autres origines en production
        ));
        
        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Headers autorisés dans les requêtes
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Headers exposés dans les réponses
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Total-Count",
            "X-Page-Number"
        ));
        
        // Autoriser les credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);
        
        // Durée de cache de la requête preflight
        configuration.setMaxAge(3600L);  // 1 heure
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
