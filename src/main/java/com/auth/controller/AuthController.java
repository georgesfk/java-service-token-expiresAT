package com.auth.controller;

import com.auth.dto.LoginRequest;
import com.auth.dto.RefreshTokenRequest;
import com.auth.dto.UserInfoResponse;
import com.auth.service.AuthService;
import com.auth.service.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Contrôleur pour les opérations d'authentification
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    /**
     * Endpoint de connexion
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("LOGIN_FAILED", e.getMessage()));
        }
    }

    /**
     * Endpoint de rafraîchissement du token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authService.refresh(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du refresh: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("REFRESH_FAILED", e.getMessage()));
        }
    }

    /**
     * Endpoint de logout
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            authService.logout(request.getRefreshToken());
            return ResponseEntity.ok(new SuccessResponse("LOGOUT_SUCCESS", "Logout réussi"));
        } catch (Exception e) {
            log.error("Erreur lors du logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("LOGOUT_FAILED", e.getMessage()));
        }
    }

    /**
     * Endpoint de logout complet (tous les appareils)
     * POST /api/auth/logout-all
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("NOT_AUTHENTICATED", "Utilisateur non authentifié"));
            }
            
            String username = authentication.getName();
            authService.logoutAll(username);
            return ResponseEntity.ok(new SuccessResponse("LOGOUT_ALL_SUCCESS", "Logout complet réussi"));
        } catch (Exception e) {
            log.error("Erreur lors du logout complet: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("LOGOUT_ALL_FAILED", e.getMessage()));
        }
    }

    /**
     * Endpoint pour obtenir les informations de l'utilisateur actuel
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("NOT_AUTHENTICATED", "Utilisateur non authentifié"));
        }

        String username = authentication.getName();
        String[] roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toArray(String[]::new);

        UserInfoResponse userInfo = UserInfoResponse.builder()
            .username(username)
            .enabled(true)
            .roles(roles)
            .build();

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Endpoint de healthcheck
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new SuccessResponse("OK", "Service d'authentification est opérationnel"));
    }

    /**
     * Classe interne pour les réponses d'erreur
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }

    /**
     * Classe interne pour les réponses de succès
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SuccessResponse {
        private String code;
        private String message;
    }
}
