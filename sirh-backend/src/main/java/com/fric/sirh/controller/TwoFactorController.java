package com.fric.sirh.controller;

import com.fric.sirh.dto.*;
import com.fric.sirh.exception.BusinessException;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.UserRepository;
import com.fric.sirh.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Generates a 2FA secret and QR code for the given user identifier (email or ID).
     */
    @PostMapping("/generate/{identifier}")
    public ResponseEntity<?> generateSecret(@PathVariable String identifier) {
        try {
            log.info("Generating 2FA secret for identifier: {}", identifier);

            User user = findUserByIdentifier(identifier);

            TwoFactorSetupDTO response = userService.generateTwoFactorSecret(user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating 2FA secret", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verifies the OTP and enables 2FA for the user. Accessible without authentication.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyAndEnable(@RequestBody TwoFactorVerifyDTO request) {
        try {
            log.info("Verifying 2FA for identifier: {}", request.getUserId());

            User user = findUserByIdentifier(request.getUserId());

            userService.verifyAndEnableTwoFactor(user.getId(), request.getOtpCode());
            return ResponseEntity.ok(Map.of("message", "2FA activated successfully"));
        } catch (Exception e) {
            log.error("Error verifying 2FA", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Initiates 2FA for a user (RH only). Generates a secret and stores it in pending state.
     */
    @PostMapping("/admin/initiate/{identifier}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<?> initiateTwoFactor(@PathVariable String identifier) {
        try {
            log.info("Initiating 2FA by RH for identifier: {}", identifier);
            User user = findUserByIdentifier(identifier);
            userService.initiateTwoFactor(user.getId());
            return ResponseEntity.ok(Map.of("message", "2FA initiated successfully. The employee must activate it from their space."));
        } catch (Exception e) {
            log.error("Error initiating 2FA", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retrieves pending 2FA information for a given user (QR code, secret, backup codes).
     * Accessible without authentication via the userId parameter.
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingTwoFactor(@RequestParam String userId) {
        try {
            log.info("Retrieving pending 2FA info for user: {}", userId);
            Map<String, Object> info = userService.getPendingTwoFactorInfo(userId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Error retrieving pending 2FA info", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Enables 2FA for a user by RH (deprecated, use /admin/initiate instead).
     */
    @PostMapping("/admin/enable/{identifier}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<?> enableByRH(@PathVariable String identifier) {
        try {
            log.info("Enabling 2FA by RH for identifier: {}", identifier);
            User user = findUserByIdentifier(identifier);
            TwoFactorSetupDTO response = userService.enableTwoFactorByRH(user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error enabling 2FA by RH", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Enables 2FA for all users (RH only).
     */
    @PostMapping("/admin/enable-all")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<?> enableForAllUsers() {
        try {
            TwoFactorMassEnableDTO response = userService.enableTwoFactorForAllUsers();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error enabling 2FA for all users", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Disables 2FA for a user (RH only).
     */
    @PostMapping("/admin/disable/{identifier}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<?> disable(@PathVariable String identifier) {
        try {
            log.info("Disabling 2FA for identifier: {}", identifier);
            User user = findUserByIdentifier(identifier);
            userService.disableTwoFactor(user.getId());
            return ResponseEntity.ok(Map.of("message", "2FA disabled successfully"));
        } catch (Exception e) {
            log.error("Error disabling 2FA", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Checks whether 2FA is enabled for a user.
     * This endpoint is public (no authentication required).
     */
    @GetMapping("/status/{identifier}")
    public ResponseEntity<?> getStatus(@PathVariable String identifier) {
        try {
            log.info("Checking 2FA status for identifier: {}", identifier);
            User user = findUserByIdentifier(identifier);
            boolean enabled = userService.isTwoFactorEnabled(user.getId());
            return ResponseEntity.ok(Map.of("twoFactorEnabled", enabled));
        } catch (Exception e) {
            log.error("Error checking 2FA status", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verifies a backup code.
     */
    @PostMapping("/verify-backup")
    public ResponseEntity<?> verifyBackup(@RequestBody TwoFactorBackupDTO request) {
        try {
            log.info("Verifying backup code for identifier: {}", request.getUserId());
            User user = findUserByIdentifier(request.getUserId());
            boolean isValid = userService.verifyBackupCode(user.getId(), request.getBackupCode());
            if (isValid) {
                return ResponseEntity.ok(Map.of("message", "Backup code is valid"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid backup code"));
            }
        } catch (Exception e) {
            log.error("Error verifying backup code", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================
    // HELPERS
    // =========================

    /**
     * Finds a user by email or ID.
     */
    private User findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new BusinessException("User identifier is required");
        }

        Optional<User> userByEmail = userRepository.findByEmail(identifier);
        if (userByEmail.isPresent()) {
            return userByEmail.get();
        }

        Optional<User> userById = userRepository.findById(identifier);
        if (userById.isPresent()) {
            return userById.get();
        }

        throw new BusinessException("User not found with identifier: " + identifier);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        return userService.findByEmail(authentication.getName());
    }
}