package com.fric.sirh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String id;

    private String firstName;
    private String lastName;
    private String email;

    private Boolean active;

    // =========================
    // ROLE INFO (UI ONLY)
    // =========================
    private String roleName;
    private Integer roleLevel;

    // =========================
    // SECURITY
    // =========================
    private List<String> permissions;

    // =========================
    // RELATION RH
    // =========================
    private String employeeId;

    // =========================
    // 2FA
    // =========================
    private Boolean twoFactorEnabled;

    // =========================
    // AUDIT
    // =========================
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}