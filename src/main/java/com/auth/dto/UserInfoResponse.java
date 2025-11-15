package com.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les informations de l'utilisateur connecté
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
    private String username;
    private String email;
    private boolean enabled;
    private String[] roles;
}
