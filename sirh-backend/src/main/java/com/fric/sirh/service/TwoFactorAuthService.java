package com.fric.sirh.service;

import com.fric.sirh.model.User;
import com.fric.sirh.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    private final UserRepository userRepository;
    private final GoogleAuthenticator googleAuthenticator;
    private final UserService userService;

    private static final String ISSUER = "SIRH Application";

    /**
     * Génère un secret 2FA pour un utilisateur avec QR code
     */
    @Transactional
    public Map<String, Object> generateTwoFactorSecret(String userId) {
        User user = userService.findById(userId);
        User currentUser = getCurrentUser();

        // Vérifier les permissions
        if (!hasPermissionToManage(user, currentUser)) {
            throw new SecurityException("Vous n'avez pas les droits pour gérer le 2FA de cet utilisateur");
        }

        if (user.isTwoFactorEnabled()) {
            throw new IllegalStateException("Le 2FA est déjà activé pour cet utilisateur");
        }

        // Générer le secret
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        // Générer l'URL du QR code
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                ISSUER,
                user.getEmail(),
                key
        );

        // Sauvegarder temporairement le secret
        user.setTwoFactorSecret(secret);
        user.setTwoFactorVerified(false);
        userRepository.save(user);

        // Générer les codes de backup
        Map<String, Object> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCodeUrl", qrCodeUrl);
        response.put("backupCodes", generateBackupCodes());

        log.info("Secret 2FA généré pour l'utilisateur {} par {}", user.getEmail(), currentUser.getEmail());

        return response;
    }

    /**
     * Vérifie et active le 2FA après scan du QR code
     */
    @Transactional
    public void verifyAndEnableTwoFactor(String userId, int otpCode) {
        User user = userService.findById(userId);
        User currentUser = getCurrentUser();

        if (!hasPermissionToManage(user, currentUser)) {
            throw new SecurityException("Vous n'avez pas les droits pour gérer le 2FA de cet utilisateur");
        }

        if (user.isTwoFactorEnabled()) {
            throw new IllegalStateException("Le 2FA est déjà activé");
        }

        if (user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("Aucun secret 2FA généré. Veuillez d'abord générer un secret.");
        }

        // Vérifier le code OTP
        boolean isValid = googleAuthenticator.authorize(user.getTwoFactorSecret(), otpCode);

        if (!isValid) {
            throw new IllegalArgumentException("Code OTP invalide");
        }

        // Activer le 2FA
        user.enableTwoFactor(user.getTwoFactorSecret(), currentUser.getId());
        user.setTwoFactorVerified(true);
        userRepository.save(user);

        log.info("2FA activé pour l'utilisateur {} par {}", user.getEmail(), currentUser.getEmail());
    }

    /**
     * Initie le processus 2FA pour un utilisateur (par un RH).
     * Génère un secret et le stocke en attente de vérification.
     * L'employé devra ensuite scanner le QR et entrer le code depuis son espace.
     */
    @Transactional
    public void initiateTwoFactorForUser(String userId) {
        User user = userService.findById(userId);
        User currentUser = getCurrentUser();

        if (!isRH(currentUser)) {
            throw new SecurityException("Seul un RH peut initier le 2FA pour un autre utilisateur");
        }
        if (user.isTwoFactorEnabled()) {
            throw new IllegalStateException("Le 2FA est déjà activé pour cet utilisateur");
        }

        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        user.setTwoFactorSecret(secret);
        user.setTwoFactorVerified(false);
        userRepository.save(user);

        log.info("2FA initié pour l'utilisateur {} par {}", user.getEmail(), currentUser.getEmail());
    }

    /**
     * Récupère les informations de 2FA en attente pour l'utilisateur courant.
     * Retourne le QR code, le secret et les codes de backup.
     */
    public Map<String, Object> getPendingTwoFactorInfo(String userId) {
        User user = userService.findById(userId);
        User currentUser = getCurrentUser();

        if (!currentUser.getId().equals(user.getId())) {
            throw new SecurityException("Vous ne pouvez accéder qu'à vos propres informations 2FA");
        }

        if (user.isTwoFactorEnabled()) {
            throw new IllegalStateException("Le 2FA est déjà activé");
        }
        if (user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("Aucun secret 2FA en attente. Contactez votre RH.");
        }

        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder(user.getTwoFactorSecret()).build();
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                ISSUER,
                user.getEmail(),
                key
        );

        Map<String, Object> response = new HashMap<>();
        response.put("secret", user.getTwoFactorSecret());
        response.put("qrCodeUrl", qrCodeUrl);
        response.put("backupCodes", generateBackupCodes());

        return response;
    }

    /**
     * Active le 2FA pour un utilisateur (par un RH) - Déprécié.
     * Utiliser plutôt initiateTwoFactorForUser pour ne pas court-circuiter la validation par l'utilisateur.
     */
    @Deprecated
    @Transactional
    public Map<String, Object> enableTwoFactorByRH(String userId) {
        initiateTwoFactorForUser(userId);
        return Map.of("message", "Le 2FA a été initié. L'utilisateur doit maintenant l'activer depuis son espace.");
    }

    /**
     * Active le 2FA pour tous les utilisateurs (par un RH)
     */
    @Transactional
    public Map<String, Object> enableTwoFactorForAllUsers() {
        User currentUser = getCurrentUser();

        if (!isRH(currentUser)) {
            throw new SecurityException("Seul un utilisateur avec le rôle RH peut activer le 2FA pour tous");
        }

        List<User> users = userRepository.findAll();
        int activatedCount = 0;
        Map<String, String> userSecrets = new HashMap<>();

        for (User user : users) {
            if (!user.isTwoFactorEnabled()) {
                GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
                String secret = key.getKey();

                user.enableTwoFactor(secret, currentUser.getId());
                user.setTwoFactorVerified(true);
                userRepository.save(user);

                userSecrets.put(user.getEmail(), secret);
                activatedCount++;
            }
        }

        log.info("2FA activé pour {} utilisateurs par {}", activatedCount, currentUser.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("activatedCount", activatedCount);
        response.put("userSecrets", userSecrets);

        return response;
    }

    /**
     * Désactive le 2FA pour un utilisateur (seul le RH peut désactiver)
     */
    @Transactional
    public void disableTwoFactor(String userId) {
        User user = userService.findById(userId);
        User currentUser = getCurrentUser();

        // Seul un RH peut désactiver le 2FA
        if (!isRH(currentUser)) {
            throw new SecurityException("Seul un utilisateur avec le rôle RH peut désactiver le 2FA");
        }

        if (!user.isTwoFactorEnabled()) {
            throw new IllegalStateException("Le 2FA n'est pas activé pour cet utilisateur");
        }

        user.disableTwoFactor(currentUser.getId());
        userRepository.save(user);

        log.info("2FA désactivé pour l'utilisateur {} par {}", user.getEmail(), currentUser.getEmail());
    }

    /**
     * Vérifie le code OTP lors de la connexion
     */
    public boolean verifyOTP(String userId, int otpCode) {
        User user = userService.findById(userId);

        if (!user.isTwoFactorEnabled()) {
            return true;
        }

        return googleAuthenticator.authorize(user.getTwoFactorSecret(), otpCode);
    }

    /**
     * Vérifie un code de backup
     */
    public boolean verifyBackupCode(String userId, String backupCode) {
        User user = userService.findById(userId);

        if (!user.isTwoFactorEnabled()) {
            return false;
        }

        boolean isValid = user.useBackupCode(backupCode);
        if (isValid) {
            userRepository.save(user);
            log.info("Code de backup utilisé pour l'utilisateur {}", user.getEmail());
        }

        return isValid;
    }

    /**
     * Vérifie si l'utilisateur a activé le 2FA
     */
    public boolean isTwoFactorEnabled(String userId) {
        User user = userService.findById(userId);
        return user.isTwoFactorEnabled();
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié");
        }
        return userService.findByEmail(authentication.getName());
    }

    private boolean isRH(User user) {
        return user.getRoleId() != null && "RH".equalsIgnoreCase(user.getRoleId());
    }

    private boolean hasPermissionToManage(User targetUser, User currentUser) {
        // Un utilisateur peut gérer son propre 2FA ou un RH peut gérer celui des autres
        return currentUser.getId().equals(targetUser.getId()) || isRH(currentUser);
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
}