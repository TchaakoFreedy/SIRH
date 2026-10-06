package com.fric.sirh.controller;

import com.fric.sirh.dto.*;
import com.fric.sirh.security.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("🔐 Login request for: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<AuthResponse> verifyTwoFactor(@RequestBody TwoFactorVerifyDTO request) {
        log.info("🔐 2FA verification for userId: {}", request.getUserId());
        return ResponseEntity.ok(authService.verifyTwoFactor(request.getUserId(), request.getOtpCode()));
    }

    @PostMapping("/refresh-token")  // ✅ Changed from "/refresh" to "/refresh-token"
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("🔄 Refresh token request received");
        return ResponseEntity.ok(authService.refreshAccessToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("🚪 Logout request");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        log.info("📧 Forgot password request for: {}", request.getEmail());
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                new ApiResponse(true, "Code de réinitialisation envoyé par email")
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("🔐 Reset password request for: {}", request.getEmail());
        authService.resetPassword(request);
        return ResponseEntity.ok(
                new ApiResponse(true, "Mot de passe modifié avec succès")
        );
    }
}