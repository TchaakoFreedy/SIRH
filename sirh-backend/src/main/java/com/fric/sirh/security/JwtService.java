package com.fric.sirh.security;

import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh.expiration}")
    private Long refreshExpiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // =========================
    // ACCESS TOKEN AVEC PERMISSIONS
    // =========================
    public String generateAccessToken(User user, Role role, List<String> permissions) {
        log.info("🔐 Génération du token pour: {}", user.getEmail());
        log.info("   - Role: {}", role.getName());
        log.info("   - Permissions: {}", permissions);

        // ✅ S'assurer que permissions n'est pas null
        List<String> perms = permissions != null ? permissions : Collections.emptyList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", role.getName());
        claims.put("roleLevel", role.getHierarchyLevel());
        claims.put("permissions", perms);
        claims.put("authorities", perms);  // ✅ Ajouter aussi comme authorities

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================
    // REFRESH TOKEN
    // =========================
    public String generateRefreshToken(User user) {
        log.info("🔄 Génération du refresh token pour: {}", user.getEmail());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================
    // VALIDATION
    // =========================
    public boolean isValid(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("⚠️ Token null ou vide");
            return false;
        }

        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            boolean isValid = expiration.after(new Date());

            if (!isValid) {
                log.warn("⚠️ Token expiré à: {}", expiration);
            }
            return isValid;
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ Token expiré: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("⚠️ Token invalide: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur validation token: {}", e.getMessage());
            return false;
        }
    }

    // =========================
    // EXTRACTION
    // =========================
    public String extractUserId(String token) {
        try {
            Claims claims = extractClaims(token);
            // ✅ Essayer plusieurs clés
            String userId = claims.getSubject();
            if (userId == null) {
                userId = claims.get("userId", String.class);
            }
            return userId;
        } catch (Exception e) {
            log.error("❌ Erreur extraction userId: {}", e.getMessage());
            return null;
        }
    }

    public String extractEmail(String token) {
        try {
            return extractClaims(token).get("email", String.class);
        } catch (Exception e) {
            log.error("❌ Erreur extraction email: {}", e.getMessage());
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            return extractClaims(token).get("role", String.class);
        } catch (Exception e) {
            log.error("❌ Erreur extraction role: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        try {
            Claims claims = extractClaims(token);

            // ✅ Essayer plusieurs clés
            Object perms = claims.get("permissions");
            if (perms == null) {
                perms = claims.get("authorities");
            }
            if (perms == null) {
                perms = claims.get("authorities", List.class);
            }

            if (perms == null) {
                log.warn("⚠️ Aucune permission trouvée dans le token");
                return Collections.emptyList();
            }

            if (perms instanceof List<?>) {
                List<String> result = ((List<?>) perms).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                log.debug("✅ Permissions extraites: {}", result);
                return result;
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ Erreur extraction permissions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}