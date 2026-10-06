package com.fric.sirh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private boolean success;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserDTO user;

    // Champs 2FA
    private Boolean twoFactorRequired;
    private Boolean twoFactorEnabled;
    private Boolean twoFactorPending;   // Add this field
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String message;
}