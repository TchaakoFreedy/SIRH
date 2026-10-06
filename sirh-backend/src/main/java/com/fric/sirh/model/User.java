package com.fric.sirh.model;

import lombok.*;
import lombok.Builder.Default;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id;

    private String firstName;
    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String password;

    @Default
    private Boolean active = true;

    private String roleId;
    private String employeeId;

    @Default
    private Set<String> grantedPermissionIds = new HashSet<>();

    @Default
    private Set<String> revokedPermissionIds = new HashSet<>();

    private LocalDateTime lastLogin;

    @Default
    private Integer loginAttempts = 0;

    @Default
    private Boolean locked = false;

    private LocalDateTime lockExpiryDate;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // ==================== 2FA FIELDS ====================

    @Default
    private Boolean twoFactorEnabled = false;

    private String twoFactorSecret;

    @Default
    private Boolean twoFactorVerified = false;

    private LocalDateTime twoFactorEnabledAt;
    private String twoFactorEnabledBy;
    private LocalDateTime twoFactorDisabledAt;
    private String twoFactorDisabledBy;

    @Default
    private Set<String> backupCodes = new HashSet<>();

    // ==================== MÉTHODES UTILITAIRES ====================

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    // ==================== 2FA METHODS ====================

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled != null && twoFactorEnabled;
    }

    public void enableTwoFactor(String secret, String enabledBy) {
        this.twoFactorEnabled = true;
        this.twoFactorSecret = secret;
        this.twoFactorVerified = false;
        this.twoFactorEnabledAt = LocalDateTime.now();
        this.twoFactorEnabledBy = enabledBy;
        this.twoFactorDisabledAt = null;
        this.twoFactorDisabledBy = null;
        this.backupCodes = generateBackupCodes();
    }

    public void disableTwoFactor(String disabledBy) {
        this.twoFactorEnabled = false;
        this.twoFactorSecret = null;
        this.twoFactorVerified = false;
        this.twoFactorDisabledAt = LocalDateTime.now();
        this.twoFactorDisabledBy = disabledBy;
        this.backupCodes.clear();
    }

    private Set<String> generateBackupCodes() {
        Set<String> codes = new HashSet<>();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 8; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                int index = (int) (Math.random() * chars.length());
                code.append(chars.charAt(index));
                if (j == 3) code.append("-");
            }
            codes.add(code.toString());
        }
        return codes;
    }

    public boolean useBackupCode(String code) {
        if (backupCodes.contains(code)) {
            backupCodes.remove(code);
            return true;
        }
        return false;
    }

    // ==================== IMPLÉMENTATION UserDetails ====================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new HashSet<>();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return locked != null ? !locked : true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active != null ? active : true;
    }
}