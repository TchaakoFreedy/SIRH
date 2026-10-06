package com.fric.sirh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;

    // Rôle de l'utilisateur
    private String roleName;
    private Integer roleLevel;

    // Permissions de l'utilisateur
    private List<String> permissions;

    // Rôles visibles dans le dropdown (selon hiérarchie)
    private List<String> visibleRoles;

    // Info employé (si l'user est lié à un employé)
    private String employeeId;

    // 2FA
    private Boolean twoFactorEnabled;

    // Indique que le 2FA est requis pour finaliser la connexion (déjà activé)
    private Boolean twoFactorRequired;

    // Indique qu'un secret 2FA est en attente d'activation (initié par RH)
    private Boolean twoFactorPending;

    // Token temporaire pour la session 2FA (optionnel)
    private String tempToken;
}