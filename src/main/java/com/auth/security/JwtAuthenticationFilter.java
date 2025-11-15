package com.auth.security;

import com.auth.service.JwtUtilImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtre JWT pour valider les tokens lors de chaque requête
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtilImpl jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extraire le JWT du header Authorization
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // Valider le token
                if (jwtUtil.isTokenExpired(jwt)) {
                    log.warn("Token expiré détecté");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Extraire le username du token
                String username = jwtUtil.getUsernameFromToken(jwt);
                
                if (StringUtils.hasText(username) && 
                    SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    // Charger les détails utilisateur
                    var userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // Créer l'authentification
                    var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // Définir l'authentification dans le contexte de sécurité
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentification JWT réussie pour l'utilisateur: {}", username);
                }
            }
        } catch (Exception ex) {
            log.error("Erreur lors du traitement du JWT", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le JWT du header Authorization
     * Format attendu: Bearer <token>
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
