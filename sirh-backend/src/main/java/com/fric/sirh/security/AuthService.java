package com.fric.sirh.security;

import com.fric.sirh.dto.*;
import com.fric.sirh.mapper.UserMapper;
import com.fric.sirh.model.*;
import com.fric.sirh.repository.*;
import com.fric.sirh.service.EmailService;
import com.fric.sirh.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final UserService userService;

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            LoginResponse loginResponse = userService.login(request);

            // 1. Si le 2FA est déjà activé (nécessite un code OTP)
            if (loginResponse != null && Boolean.TRUE.equals(loginResponse.getTwoFactorRequired())) {
                log.info("2FA required for user: {}", request.getEmail());
                return AuthResponse.builder()
                        .success(false)
                        .twoFactorRequired(true)
                        .twoFactorEnabled(true)
                        .userId(loginResponse.getUserId())
                        .email(loginResponse.getEmail())
                        .firstName(loginResponse.getFirstName())
                        .lastName(loginResponse.getLastName())
                        .message("2FA code required")
                        .build();
            }

            // 2. Si un secret 2FA est en attente (initié par RH)
            if (loginResponse != null && Boolean.TRUE.equals(loginResponse.getTwoFactorPending())) {
                log.info("2FA pending for user: {}, redirect to activation", request.getEmail());
                return AuthResponse.builder()
                        .success(false)
                        .twoFactorPending(true)
                        .twoFactorEnabled(false)
                        .userId(loginResponse.getUserId())
                        .email(loginResponse.getEmail())
                        .firstName(loginResponse.getFirstName())
                        .lastName(loginResponse.getLastName())
                        .message("2FA activation required")
                        .build();
            }

            // 3. Login normal : récupérer l'utilisateur et générer les tokens
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid email"));

            if (!Boolean.TRUE.equals(user.getActive())) {
                log.warn("Login attempt on inactive account: {}", request.getEmail());
                return AuthResponse.builder()
                        .success(false)
                        .message("Account deactivated. Please contact administrator.")
                        .build();
            }

            if (Boolean.TRUE.equals(user.getLocked())) {
                log.warn("Login attempt on locked account: {}", request.getEmail());
                return AuthResponse.builder()
                        .success(false)
                        .message("Account locked. Please contact administrator.")
                        .build();
            }

            Role role = roleRepository.findById(user.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            List<String> permissionIds = Optional.ofNullable(role.getPermissionIds())
                    .orElse(new ArrayList<>());

            List<String> permissions = permissionRepository.findByIdIn(permissionIds)
                    .stream()
                    .map(Permission::getName)
                    .toList();

            String accessToken = jwtService.generateAccessToken(user, role, permissions);
            String refreshToken = jwtService.generateRefreshToken(user);

            refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            UserDTO userDTO = buildUserDTO(user, role, permissions);

            log.info("Login successful for user: {}", request.getEmail());

            return AuthResponse.builder()
                    .success(true)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(604800000L)
                    .user(userDTO)
                    .twoFactorEnabled(false)
                    .twoFactorRequired(false)
                    .message("Login successful")
                    .build();

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("Login error: " + e.getMessage())
                    .build();
        }
    }

    public AuthResponse verifyTwoFactor(String userId, int otpCode) {
        log.info("2FA verification for user ID: {}", userId);

        try {
            LoginResponse loginResponse = userService.completeLoginWith2FA(userId, otpCode);

            if (loginResponse == null) {
                log.warn("2FA verification failed for user ID: {}", userId);
                return AuthResponse.builder()
                        .success(false)
                        .message("Invalid or expired 2FA code")
                        .build();
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!Boolean.TRUE.equals(user.getActive())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("Account deactivated")
                        .build();
            }

            Role role = roleRepository.findById(user.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            List<String> permissionIds = Optional.ofNullable(role.getPermissionIds())
                    .orElse(new ArrayList<>());

            List<String> permissions = permissionRepository.findByIdIn(permissionIds)
                    .stream()
                    .map(Permission::getName)
                    .toList();

            String accessToken = jwtService.generateAccessToken(user, role, permissions);
            String refreshToken = jwtService.generateRefreshToken(user);

            refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            UserDTO userDTO = buildUserDTO(user, role, permissions);

            log.info("2FA verification successful for user: {}", user.getEmail());

            return AuthResponse.builder()
                    .success(true)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(604800000L)
                    .user(userDTO)
                    .twoFactorEnabled(true)
                    .twoFactorRequired(false)
                    .message("2FA login successful")
                    .build();

        } catch (Exception e) {
            log.error("2FA verification error: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("2FA verification error: " + e.getMessage())
                    .build();
        }
    }

    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        log.info("Refresh token requested");

        try {
            String refreshToken = request.getRefreshToken();

            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("Refresh token missing");
                return AuthResponse.builder()
                        .success(false)
                        .message("Refresh token missing")
                        .build();
            }

            if (!jwtService.isRefreshToken(refreshToken)) {
                log.warn("Invalid token for refresh");
                return AuthResponse.builder()
                        .success(false)
                        .message("Invalid token for refresh")
                        .build();
            }

            if (!jwtService.isValid(refreshToken)) {
                log.warn("Refresh token expired");
                return AuthResponse.builder()
                        .success(false)
                        .message("Refresh token expired")
                        .build();
            }

            String userId = jwtService.extractUserId(refreshToken);
            if (userId == null || userId.isEmpty()) {
                log.warn("Unable to extract userId from refresh token");
                return AuthResponse.builder()
                        .success(false)
                        .message("Invalid refresh token")
                        .build();
            }

            if (!refreshTokenService.isValidRefreshToken(userId, refreshToken)) {
                log.warn("Invalid refresh token for userId: {}", userId);
                return AuthResponse.builder()
                        .success(false)
                        .message("Invalid refresh token")
                        .build();
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!Boolean.TRUE.equals(user.getActive())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("Account deactivated")
                        .build();
            }

            Role role = roleRepository.findById(user.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            List<String> permissionIds = Optional.ofNullable(role.getPermissionIds())
                    .orElse(new ArrayList<>());

            List<String> permissions = permissionRepository.findByIdIn(permissionIds)
                    .stream()
                    .map(Permission::getName)
                    .toList();

            String newAccessToken = jwtService.generateAccessToken(user, role, permissions);

            UserDTO userDTO = buildUserDTO(user, role, permissions);

            log.info("Refresh token successful for user: {}", user.getEmail());

            return AuthResponse.builder()
                    .success(true)
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(604800000L)
                    .user(userDTO)
                    .message("Token refreshed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Refresh token error: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("Refresh token error: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    public void logout(String userId) {
        log.info("Logout for userId: {}", userId);

        if (userId != null) {
            refreshTokenService.revokeRefreshToken(userId);
            log.info("Refresh token revoked for userId: {}", userId);
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        passwordResetTokenRepository.deleteByEmail(user.getEmail());

        String code = String.format("%06d", new SecureRandom().nextInt(999999));

        PasswordResetToken token = PasswordResetToken.builder()
                .email(user.getEmail())
                .code(code)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();

        passwordResetTokenRepository.save(token);
        emailService.sendResetPasswordCode(user.getEmail(), code);

        log.info("Password reset code sent to: {}", request.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset for email: {}", request.getEmail());

        PasswordResetToken token = passwordResetTokenRepository
                .findByEmailAndCode(request.getEmail(), request.getCode())
                .orElseThrow(() -> new RuntimeException("Invalid code"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Code expired");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
        passwordResetTokenRepository.delete(token);

        log.info("Password reset successful for: {}", request.getEmail());
    }

    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Token is null or empty");
            return false;
        }

        try {
            boolean isValid = jwtService.isValid(token);
            log.debug("Token validation result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    private UserDTO buildUserDTO(User user, Role role, List<String> permissions) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .active(user.getActive())
                .employeeId(user.getEmployeeId())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .roleName(role.getName())
                .roleLevel(role.getHierarchyLevel())
                .permissions(permissions != null ? permissions : Collections.emptyList())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .build();
    }
}