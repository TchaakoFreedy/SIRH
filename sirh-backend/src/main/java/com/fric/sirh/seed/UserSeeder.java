package com.fric.sirh.seed;

import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("rh@system.com")) {
            log.info("ℹ️ Utilisateur RH déjà existant");
            return;
        }

        Role rh = roleRepository.findByName("RH")
                .orElseThrow(() -> new RuntimeException("RH role not found"));

        User admin = User.builder()
                .firstName("RESOURCES")
                .lastName("HUMAINES")
                .email("rh@system.com")
                .password(passwordEncoder.encode("123456"))
                .roleId(rh.getId())
                .active(true)
                .grantedPermissionIds(new HashSet<>())
                .revokedPermissionIds(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .createdBy("SYSTEM")
                .build();

        userRepository.save(admin);
        log.info("✅ Utilisateur RH créé avec succès");
    }
}