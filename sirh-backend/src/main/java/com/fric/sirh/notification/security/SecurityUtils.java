package com.fric.sirh.notification.security;

import com.fric.sirh.security.CustomUserDetails;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.error("Aucune authentification trouvee dans le contexte");
            throw new IllegalStateException("User not authenticated");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            String userId = ((CustomUserDetails) principal).getId();
            log.debug("ID utilisateur extrait de CustomUserDetails : {}", userId);
            return userId;
        }

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            log.debug("Recherche de l'utilisateur par email : {}", username);
            return userRepository.findByEmail(username)
                    .map(User::getId)
                    .orElseThrow(() -> new IllegalStateException("User not found by email: " + username));
        }

        String name = auth.getName();
        log.warn("Impossible de recuperer l'ID utilisateur, retour de l'email : {}", name);
        return name;
    }

    public String getCurrentUserCompanyId() {
        String userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmployeeId() == null) return null;
        Employee employee = employeeRepository.findById(user.getEmployeeId()).orElse(null);
        return employee != null ? employee.getEntrepriseId() : null;
    }

    public String getCurrentUserDepartmentId() {
        String userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmployeeId() == null) return null;
        Employee employee = employeeRepository.findById(user.getEmployeeId()).orElse(null);
        return employee != null ? employee.getDepartementId() : null;
    }
}