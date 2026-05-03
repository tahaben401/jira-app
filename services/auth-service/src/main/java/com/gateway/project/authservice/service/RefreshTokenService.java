package com.gateway.project.authservice.service;

import com.gateway.project.authservice.entity.RefreshToken;
import com.gateway.project.authservice.entity.User;
import com.gateway.project.authservice.exception.AuthException;
import com.gateway.project.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // ms

    private static final int MAX_ACTIVE_SESSIONS = 5;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public RefreshToken create(User user) {
        long activeSessions = refreshTokenRepository.countByUserAndRevokedFalse(user);
        if (activeSessions >= MAX_ACTIVE_SESSIONS) {
            log.warn("Max sessions reached for user {}. Revoking all.", user.getEmail());
            refreshTokenRepository.revokeAllByUser(user);
        }

        RefreshToken token = RefreshToken.builder()
                .token(generateToken())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(token);
    }

    // ── Validate ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RefreshToken validate(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new AuthException.UnauthorizedException("Refresh token not found"));

        if (token.isRevoked()) {
            // Reuse detected — nuke all sessions for this user (theft prevention)
            log.warn("Revoked token reuse detected for user: {}", token.getUser().getEmail());
            refreshTokenRepository.revokeAllByUser(token.getUser());
            throw new AuthException.UnauthorizedException("Token reuse detected. All sessions have been revoked. Please log in again.");
        }

        if (token.isExpired()) {
            throw new AuthException.UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        return token;
    }

    // ── Rotate (validate old → revoke → issue new) ────────────────────────────

    @Transactional
    public RefreshToken rotate(String oldTokenValue) {
        RefreshToken old = validate(oldTokenValue);
        old.setRevoked(true);
        refreshTokenRepository.save(old);
        return create(old.getUser());
    }

    // ── Revoke one token (single-device logout) ───────────────────────────────

    @Transactional
    public void revoke(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    // ── Revoke all tokens for user (all-devices logout) ───────────────────────

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    // ── Scheduled cleanup (runs daily at 02:00) ───────────────────────────────

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpired() {
        log.info("Cleaning up expired/revoked refresh tokens...");
        refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
