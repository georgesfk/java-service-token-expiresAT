package com.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entité pour les refresh tokens
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token", unique = true),
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private Instant expiresAt;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;
    
    @Column
    private Instant revokedAt;
}
