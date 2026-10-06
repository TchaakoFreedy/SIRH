package com.fric.sirh.security;

import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.PermissionRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import com.fric.sirh.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final EmployeeRepository employeeRepository;
    private final UserPermissionService userPermissionService;

    private final ConcurrentHashMap<String, List<GrantedAuthority>> authorityCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10 * 60 * 1000;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by identifier: {}", username);

        User user = null;

        if (username != null && !username.isEmpty()) {
            boolean isPossibleObjectId = username.matches("^[a-fA-F0-9]{24}$");

            if (isPossibleObjectId) {
                log.debug("Attempting to find user by ID: {}", username);
                user = userRepository.findById(username).orElse(null);
                if (user != null) {
                    log.debug("User found by ID: {}", user.getEmail());
                }
            }

            if (user == null) {
                log.debug("Attempting to find user by email: {}", username);
                user = userRepository.findByEmail(username).orElse(null);
                if (user != null) {
                    log.debug("User found by email: {}", user.getEmail());
                }
            }
        }

        if (user == null) {
            log.error("User not found: {}", username);
            throw new UsernameNotFoundException("Utilisateur introuvable : " + username);
        }

        return buildUserDetails(user);
    }

    public UserDetails loadUserById(String id) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found by ID: {}", id);
                    return new UsernameNotFoundException("Utilisateur introuvable : " + id);
                });

        log.debug("User found by ID: {}", user.getEmail());
        return buildUserDetails(user);
    }

    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found by email: {}", email);
                    return new UsernameNotFoundException("Utilisateur introuvable : " + email);
                });

        log.debug("User found by email: {}", user.getEmail());
        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        log.info("Building UserDetails for: {} (ID: {})", user.getEmail(), user.getId());

        if (!Boolean.TRUE.equals(user.getActive())) {
            log.warn("Compte desactive: {}", user.getEmail());
            throw new UsernameNotFoundException("Compte desactive");
        }

        employeeRepository.findByUserId(user.getId()).ifPresent(employee -> {
            if ("SUSPENDU".equals(employee.getStatut())) {
                log.warn("Tentative d'acces compte suspendu: {}", user.getEmail());
                throw new UsernameNotFoundException("Compte suspendu (Employe inactif)");
            }
        });

        Collection<GrantedAuthority> authorities = getAuthoritiesForUser(user);

        log.info("Utilisateur {} charge avec {} autorites au total", user.getEmail(), authorities.size());

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getActive(),
                true,
                true,
                !Boolean.TRUE.equals(user.getLocked()),
                authorities
        );
    }

    private Collection<GrantedAuthority> getAuthoritiesForUser(User user) {
        String cacheKey = user.getId();

        cleanCache();

        if (authorityCache.containsKey(cacheKey)) {
            log.debug("Autorites en cache pour l'utilisateur: {}", user.getEmail());
            return authorityCache.get(cacheKey);
        }

        log.info("Construction des autorites pour l'utilisateur: {}", user.getEmail());

        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getRoleId() != null) {
            roleRepository.findById(user.getRoleId()).ifPresent(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                log.debug("Role ajoute: ROLE_{}", role.getName());
            });
        }

        Set<String> effectivePermissionNames = userPermissionService.calculateEffectivePermissionNames(user);
        log.info("Permissions effectives pour {}: {}", user.getEmail(), effectivePermissionNames);

        for (String permName : effectivePermissionNames) {
            authorities.add(new SimpleGrantedAuthority(permName));
        }

        if (user.getRoleId() != null) {
            roleRepository.findById(user.getRoleId()).ifPresent(role -> {
                if ("RH".equals(role.getName())) {
                    List<String> allPermissions = getCachedAllPermissions();
                    for (String permName : allPermissions) {
                        authorities.add(new SimpleGrantedAuthority(permName));
                    }
                    log.info("RH: Toutes les permissions ajoutees ({})", allPermissions.size());
                }
            });
        }

        List<GrantedAuthority> authorityList = new ArrayList<>(authorities);
        authorityCache.put(cacheKey, authorityList);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());

        return authorityList;
    }

    private List<String> getCachedAllPermissions() {
        String cacheKey = "ALL_PERMISSIONS";

        if (authorityCache.containsKey(cacheKey)) {
            List<GrantedAuthority> cached = authorityCache.get(cacheKey);
            return cached.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(java.util.stream.Collectors.toList());
        }

        List<String> allPermissions = permissionRepository.findAll()
                .stream()
                .map(p -> p.getName())
                .toList();

        List<GrantedAuthority> authorityList = allPermissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());

        authorityCache.put(cacheKey, authorityList);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());

        return allPermissions;
    }

    public void clearCache(String userId) {
        if (userId != null) {
            authorityCache.remove(userId);
            cacheTimestamps.remove(userId);
            log.info("Cache vide pour l'utilisateur: {}", userId);
        } else {
            authorityCache.clear();
            cacheTimestamps.clear();
            log.info("Cache completement vide");
        }
    }

    public void clearAllPermissionsCache() {
        authorityCache.remove("ALL_PERMISSIONS");
        cacheTimestamps.remove("ALL_PERMISSIONS");
        log.info("Cache des permissions supprime");
    }

    private void cleanCache() {
        long now = System.currentTimeMillis();
        cacheTimestamps.entrySet().removeIf(entry -> now - entry.getValue() > CACHE_TTL_MS);
        authorityCache.keySet().removeIf(key -> !cacheTimestamps.containsKey(key));
    }
}