package com.fric.sirh.security;

import com.fric.sirh.model.RefreshToken;
import com.fric.sirh.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final HttpServletRequest request;

    @Value("${jwt.refresh.expiration:604800000}")
    private Long refreshExpirationMs;

    @Transactional
    public void saveRefreshToken(String userId, String refreshToken) {
        // ✅ Supprimer l'ancien token s'il existe
        refreshTokenRepository.deleteByUserId(userId);

        // ✅ Créer le nouveau token
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .token(refreshToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(request.getRemoteAddr())
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);

        log.info("✅ Refresh token sauvegardé pour userId: {} (IP: {})",
                userId, token.getIpAddress());
    }

    @Transactional
    public boolean isValidRefreshToken(String userId, String refreshToken) {
        if (userId == null || refreshToken == null) {
            log.warn("⚠️ userId ou refreshToken null");
            return false;
        }

        return refreshTokenRepository.findByUserIdAndToken(userId, refreshToken)
                .map(token -> {
                    // ✅ Vérifier si révoqué
                    if (token.isRevoked()) {
                        log.warn("⚠️ Refresh token révoqué pour userId: {}", userId);
                        return false;
                    }

                    // ✅ Vérifier l'expiration
                    if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                        log.warn("⚠️ Refresh token expiré pour userId: {}", userId);
                        refreshTokenRepository.delete(token);
                        return false;
                    }

                    log.debug("✅ Refresh token valide pour userId: {}", userId);
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("⚠️ Aucun refresh token trouvé pour userId: {}", userId);
                    return false;
                });
    }

    @Transactional
    public void revokeRefreshToken(String userId) {
        if (userId != null) {
            refreshTokenRepository.deleteByUserId(userId);
            log.info("✅ Refresh token révoqué pour userId: {}", userId);
        }
    }

    @Transactional
    public void revokeAllUserTokens(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("✅ Tous les refresh tokens révoqués pour userId: {}", userId);
    }

    @Transactional
    public void cleanExpiredTokens() {
        // MongoDB gère automatiquement l'expiration avec @Indexed(expireAfterSeconds = 0)
        log.debug("🧹 Nettoyage automatique des tokens expirés géré par MongoDB");
    }

    public long countTokensForUser(String userId) {
        return refreshTokenRepository.countByUserId(userId);
    }
}