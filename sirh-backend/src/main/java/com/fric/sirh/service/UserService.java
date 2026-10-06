package com.fric.sirh.service;

import com.fric.sirh.dto.*;
import com.fric.sirh.exception.BusinessException;
import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Permission;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.PermissionRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import com.fric.sirh.security.JwtService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserPermissionService userPermissionService;
    private final EmployeeService employeeService;
    private final GoogleAuthenticator googleAuthenticator;

    private static final String ISSUER = "SIRH Application";

    // =========================
    // LOGIN
    // =========================
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Mot de passe incorrect");
        }

        if (!user.getActive()) {
            throw new BusinessException("Compte désactivé");
        }

        if (user.getLocked()) {
            throw new BusinessException("Compte verrouillé");
        }

        // 1. 2FA déjà activé
        if (user.isTwoFactorEnabled()) {
            return LoginResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .twoFactorEnabled(true)
                    .twoFactorRequired(true)
                    .twoFactorPending(false)
                    .build();
        }

        // 2. Secret en attente (initié par RH)
        log.info("Login - secret: {}, verified: {}, enabled: {}",
                user.getTwoFactorSecret(),
                user.getTwoFactorVerified(),
                user.isTwoFactorEnabled());

        if (user.getTwoFactorSecret() != null && !Boolean.TRUE.equals(user.getTwoFactorVerified())) {
            log.info("2FA en attente pour {}, redirection vers activation", user.getEmail());
            return LoginResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .twoFactorEnabled(false)
                    .twoFactorRequired(false)
                    .twoFactorPending(true)
                    .build();
        }

        // 3. Login normal
        return generateLoginResponse(user);
    }

    public LoginResponse completeLoginWith2FA(String userId, int otpCode) {
        User user = findUser(userId);

        if (!user.isTwoFactorEnabled()) {
            throw new BusinessException("Le 2FA n'est pas activé pour cet utilisateur");
        }

        boolean isValid = googleAuthenticator.authorize(user.getTwoFactorSecret(), otpCode);

        if (!isValid) {
            throw new BusinessException("Code OTP invalide");
        }

        user.setLastLogin(LocalDateTime.now());
        user.setLoginAttempts(0);
        userRepository.save(user);

        return generateLoginResponse(user);
    }

    private LoginResponse generateLoginResponse(User user) {
        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BusinessException("Rôle introuvable"));

        Set<String> effectivePermissionIds = userPermissionService.calculateEffectivePermissions(user);

        List<Permission> permissions = permissionRepository.findByIdIn(new ArrayList<>(effectivePermissionIds));
        List<String> permissionNames = permissions.stream()
                .map(Permission::getName)
                .toList();

        List<String> visibleRoles = getVisibleRoles(role.getHierarchyLevel());

        String token = jwtService.generateAccessToken(user, role, permissionNames);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roleName(role.getName())
                .roleLevel(role.getHierarchyLevel())
                .permissions(permissionNames)
                .visibleRoles(visibleRoles)
                .employeeId(user.getEmployeeId())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .twoFactorRequired(false)
                .build();
    }

    // =========================
    // 2FA METHODS
    // =========================

    @Transactional
    public TwoFactorSetupDTO generateTwoFactorSecret(String userId) {
        User user = findUser(userId);

        if (user.isTwoFactorEnabled()) {
            throw new BusinessException("Le 2FA est déjà activé pour cet utilisateur");
        }

        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                ISSUER,
                user.getEmail(),
                key
        );

        user.setTwoFactorSecret(secret);
        user.setTwoFactorVerified(false);
        userRepository.save(user);

        Set<String> backupCodes = generateBackupCodes();

        log.info("Secret 2FA généré pour l'utilisateur {}", user.getEmail());

        return TwoFactorSetupDTO.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .build();
    }

    @Transactional
    public void verifyAndEnableTwoFactor(String userId, int otpCode) {
        User user = findUser(userId);

        if (user.isTwoFactorEnabled()) {
            throw new BusinessException("Le 2FA est déjà activé");
        }

        if (user.getTwoFactorSecret() == null) {
            throw new BusinessException("Aucun secret 2FA généré. Veuillez d'abord générer un secret.");
        }

        boolean isValid = googleAuthenticator.authorize(user.getTwoFactorSecret(), otpCode);
        if (!isValid) {
            throw new BusinessException("Code OTP invalide");
        }

        user.enableTwoFactor(user.getTwoFactorSecret(), userId);
        user.setTwoFactorVerified(true);
        userRepository.save(user);

        log.info("2FA activé pour l'utilisateur {}", user.getEmail());
    }

    @Transactional
    public TwoFactorSetupDTO enableTwoFactorByRH(String userId) {
        User user = findUser(userId);
        User currentUser = getCurrentUser();

        if (!isRH(currentUser)) {
            throw new BusinessException("Seul un utilisateur avec le rôle RH peut activer le 2FA pour d'autres utilisateurs");
        }

        if (user.isTwoFactorEnabled()) {
            throw new BusinessException("Le 2FA est déjà activé pour cet utilisateur");
        }

        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        user.enableTwoFactor(secret, currentUser.getId());
        user.setTwoFactorVerified(true);
        userRepository.save(user);

        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                ISSUER,
                user.getEmail(),
                key
        );

        log.info("2FA activé par RH pour l'utilisateur {} par {}", user.getEmail(), currentUser.getEmail());

        return TwoFactorSetupDTO.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(user.getBackupCodes())
                .build();
    }

    @Transactional
    public TwoFactorMassEnableDTO enableTwoFactorForAllUsers() {
        User currentUser = getCurrentUser();

        if (!isRH(currentUser)) {
            throw new BusinessException("Seul un utilisateur avec le rôle RH peut activer le 2FA pour tous");
        }

        List<User> users = userRepository.findAll();
        int activatedCount = 0;
        Map<String, String> userSecrets = new HashMap<>();
        Map<String, Set<String>> userBackupCodes = new HashMap<>();

        for (User user : users) {
            if (!user.isTwoFactorEnabled()) {
                GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
                String secret = key.getKey();

                user.enableTwoFactor(secret, currentUser.getId());
                user.setTwoFactorVerified(true);
                userRepository.save(user);

                userSecrets.put(user.getEmail(), secret);
                userBackupCodes.put(user.getEmail(), user.getBackupCodes());
                activatedCount++;
            }
        }

        log.info("2FA activé pour {} utilisateurs par {}", activatedCount, currentUser.getEmail());

        return TwoFactorMassEnableDTO.builder()
                .activatedCount(activatedCount)
                .userSecrets(userSecrets)
                .userBackupCodes(userBackupCodes)
                .build();
    }

    @Transactional
    public void disableTwoFactor(String userId) {
        User user = findUser(userId);
        User currentUser = getCurrentUser();

        if (!isRH(currentUser)) {
            throw new BusinessException("Seul un utilisateur avec le rôle RH peut désactiver le 2FA");
        }

        if (!user.isTwoFactorEnabled()) {
            throw new BusinessException("Le 2FA n'est pas activé pour cet utilisateur");
        }

        user.disableTwoFactor(currentUser.getId());
        userRepository.save(user);

        log.info("2FA désactivé pour l'utilisateur {} par {}", user.getEmail(), currentUser.getEmail());
    }

    public boolean isTwoFactorEnabled(String userId) {
        User user = findUser(userId);
        return user.isTwoFactorEnabled();
    }

    @Transactional
    public boolean verifyBackupCode(String userId, String backupCode) {
        User user = findUser(userId);

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

    // =========================
    // INITIATION 2FA (RH)
    // =========================

    @Transactional
    public void initiateTwoFactor(String userId) {
        User user = findUser(userId);
        User currentUser = getCurrentUser();

        if (!isRH(currentUser)) {
            throw new BusinessException("Seul un RH peut initier le 2FA pour un autre utilisateur");
        }
        if (user.isTwoFactorEnabled()) {
            throw new BusinessException("Le 2FA est déjà activé pour cet utilisateur");
        }

        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        user.setTwoFactorSecret(secret);
        user.setTwoFactorVerified(false);
        userRepository.save(user);

        log.info("2FA initié pour l'utilisateur {} par {}", user.getEmail(), currentUser.getEmail());
    }

    /**
     * Récupère les informations de 2FA en attente pour un utilisateur.
     * Accessible sans authentification via userId.
     */
    public Map<String, Object> getPendingTwoFactorInfo(String userId) {
        User user = findUser(userId);

        if (user.isTwoFactorEnabled()) {
            throw new BusinessException("Le 2FA est déjà activé");
        }
        if (user.getTwoFactorSecret() == null) {
            throw new BusinessException("Aucun secret 2FA en attente. Contactez votre RH.");
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

    // =========================
    // USERS CRUD
    // =========================
    public List<UserDTO> getAll() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getById(String id) {
        return toDTO(findUser(id));
    }

    public User getUserById(String id) {
        return findUser(id);
    }

    public Role getRoleByUserId(String userId) {
        User user = findUser(userId);
        return roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BusinessException("Rôle non trouvé pour l'utilisateur: " + userId));
    }

    public boolean hasRole(String userId, String roleName) {
        try {
            Role role = getRoleByUserId(userId);
            return role.getName().equals(roleName);
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification du rôle pour l'utilisateur {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public boolean isSuperAdminOrTopManager(String userId) {
        try {
            Role role = getRoleByUserId(userId);
            String roleName = role.getName();
            return "SUPER_ADMIN".equals(roleName) || "TOP_MANAGER".equals(roleName);
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification du rôle pour l'utilisateur {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public boolean isDirection(String userId) {
        return hasRole(userId, "DIRECTION");
    }

    public boolean isManager(String userId) {
        return hasRole(userId, "MANAGER");
    }

    public boolean canAccessEmployee(String userId, String employeeId) {
        try {
            if (isSuperAdminOrTopManager(userId)) {
                return true;
            }

            Employee currentEmployee = employeeService.findByUserId(userId);
            if (currentEmployee == null) {
                log.warn("Aucun employé associé à l'utilisateur {}", userId);
                return false;
            }

            Employee targetEmployee = employeeService.getById(employeeId);
            if (targetEmployee == null) {
                log.warn("Employé cible non trouvé: {}", employeeId);
                return false;
            }

            if (currentEmployee.getId().equals(targetEmployee.getId())) {
                return true;
            }

            if (isDirection(userId)) {
                boolean canAccess = currentEmployee.getEntrepriseId() != null &&
                        currentEmployee.getEntrepriseId().equals(targetEmployee.getEntrepriseId());
                if (!canAccess) {
                    log.warn("Direction {} tente d'accéder à un employé d'une autre entreprise", userId);
                }
                return canAccess;
            }

            if (isManager(userId)) {
                boolean canAccess = currentEmployee.getDepartementId() != null &&
                        currentEmployee.getDepartementId().equals(targetEmployee.getDepartementId()) &&
                        currentEmployee.getEntrepriseId() != null &&
                        currentEmployee.getEntrepriseId().equals(targetEmployee.getEntrepriseId());
                if (!canAccess) {
                    log.warn("Manager {} tente d'accéder à un employé d'un autre département", userId);
                }
                return canAccess;
            }

            return false;

        } catch (Exception e) {
            log.error("Erreur lors de la vérification d'accès pour l'utilisateur {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public UserDTO create(CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email déjà utilisé");
        }

        if (request.getRoleId() != null && !roleRepository.existsById(request.getRoleId())) {
            throw new BusinessException("Rôle invalide");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roleId(request.getRoleId())
                .employeeId(request.getEmployeeId())
                .active(true)
                .grantedPermissionIds(new HashSet<>())
                .revokedPermissionIds(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .createdBy("SYSTEM")
                .build();

        User saved = userRepository.save(user);
        log.info("Utilisateur créé : {}", saved.getEmail());

        return toDTO(saved);
    }

    @Transactional
    public UserDTO update(String id, CreateUserRequest request) {
        User user = findUser(id);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new BusinessException("Email déjà utilisé");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRoleId() != null) {
            if (!roleRepository.existsById(request.getRoleId())) {
                throw new BusinessException("Rôle invalide");
            }
            user.setRoleId(request.getRoleId());
        }
        if (request.getEmployeeId() != null) {
            user.setEmployeeId(request.getEmployeeId());
        }

        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy("SYSTEM");

        User updated = userRepository.save(user);
        log.info("Utilisateur mis à jour : {}", updated.getEmail());

        return toDTO(updated);
    }

    @Transactional
    public void delete(String id) {
        User user = findUser(id);
        userRepository.delete(user);
        log.info("Utilisateur supprimé : {}", user.getEmail());
    }

    @Transactional
    public UserDTO toggleStatus(String id) {
        User user = findUser(id);
        user.setActive(!user.getActive());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy("SYSTEM");

        User updated = userRepository.save(user);
        log.info("Statut utilisateur changé : {} -> {}",
                updated.getEmail(), updated.getActive() ? "ACTIF" : "INACTIF");

        return toDTO(updated);
    }

    // =========================
    // MAPPING
    // =========================
    private UserDTO toDTO(User user) {
        Role role = roleRepository.findById(user.getRoleId()).orElse(null);

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setActive(user.getActive());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        dto.setTwoFactorEnabled(user.isTwoFactorEnabled());

        if (role != null) {
            dto.setRoleName(role.getName());
            dto.setRoleLevel(role.getHierarchyLevel());

            Set<String> effectivePermissionIds = userPermissionService.calculateEffectivePermissions(user);
            List<Permission> permissions = permissionRepository.findByIdIn(new ArrayList<>(effectivePermissionIds));
            dto.setPermissions(permissions.stream().map(Permission::getName).toList());
        }

        return dto;
    }

    // =========================
    // HELPERS
    // =========================
    private List<String> getVisibleRoles(int level) {
        return roleRepository.findAll().stream()
                .filter(r -> r.getHierarchyLevel() <= level)
                .map(Role::getName)
                .toList();
    }

    private User findUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        String principal = authentication.getName();
        User user = null;

        Optional<User> userByEmail = userRepository.findByEmail(principal);
        if (userByEmail.isPresent()) {
            user = userByEmail.get();
        } else {
            Optional<User> userById = userRepository.findById(principal);
            if (userById.isPresent()) {
                user = userById.get();
            } else {
                throw new BusinessException("Utilisateur non trouvé avec l'identifiant: " + principal);
            }
        }

        return user;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé avec l'email: " + email));
    }

    private boolean isRH(User user) {
        if (user == null || user.getRoleId() == null) {
            return false;
        }
        try {
            Role role = roleRepository.findById(user.getRoleId()).orElse(null);
            return role != null && "RH".equalsIgnoreCase(role.getName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasPermissionToManageTwoFactor(User targetUser, User currentUser) {
        return currentUser.getId().equals(targetUser.getId()) || isRH(currentUser);
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé avec l'ID: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé avec l'email: " + email));
    }

    public User findByIdentifier(String identifier) {
        Optional<User> userByEmail = userRepository.findByEmail(identifier);
        if (userByEmail.isPresent()) {
            return userByEmail.get();
        }
        Optional<User> userById = userRepository.findById(identifier);
        if (userById.isPresent()) {
            return userById.get();
        }
        throw new BusinessException("Utilisateur non trouvé avec l'identifiant: " + identifier);
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