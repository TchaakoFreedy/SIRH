package com.fric.sirh.security;

import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentUserHelper {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        String identifier = auth.getName();
        log.info("🔍 Recherche de l'utilisateur avec identifiant: {}", identifier);

        // Essayer par email d'abord
        return userRepository.findByEmail(identifier)
                .or(() -> {
                    // Si c'est un ID MongoDB (24 caractères hexadécimaux), chercher par ID
                    if (identifier.matches("^[a-fA-F0-9]{24}$")) {
                        log.info("🔍 L'identifiant ressemble à un ID MongoDB, recherche par ID: {}", identifier);
                        return userRepository.findById(identifier);
                    }
                    return Optional.empty();
                })
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'identifiant : " + identifier));
    }

    public Employee getCurrentEmployee() {
        User user = getCurrentUser();
        if (user.getEmployeeId() == null) {
            throw new RuntimeException("Aucun employé associé à cet utilisateur");
        }
        return employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec ID: " + user.getEmployeeId()));
    }
}