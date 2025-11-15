package com.auth.config;

import com.auth.security.JwtAuthenticationEntryPoint;
import com.auth.security.JwtAccessDeniedHandler;
import com.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration complète de sécurité Spring avec JWT
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * Bean pour encoder les passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean pour l'AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configuration Spring Security avec JWT
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Désactiver CSRF (pas nécessaire avec JWT)
            .csrf().disable()
            
            // Configuration CORS (à adapter si nécessaire)
            .cors()
            .and()
            
            // Utiliser le mode stateless (pas de session)
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            
            // Gestion des exceptions d'authentification
            .exceptionHandling()
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .accessDeniedHandler(jwtAccessDeniedHandler)
            .and()
            
            // Configuration des routes
            .authorizeHttpRequests(authz -> authz
                // Routes publiques
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Routes protégées
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout-all").authenticated()
                
                // Toute autre requête nécessite une authentification
                .anyRequest().authenticated()
            )
            
            // Ajouter le filtre JWT avant le filtre d'authentification par défaut
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Autoriser l'accès à la console H2 (développement uniquement)
        http.headers().frameOptions().disable();

        return http.build();
    }
}
