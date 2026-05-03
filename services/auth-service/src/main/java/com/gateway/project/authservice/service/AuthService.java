package com.gateway.project.authservice.service;

import com.gateway.project.authservice.dto.AuthDtos.*;
import com.gateway.project.authservice.entity.RefreshToken;
import com.gateway.project.authservice.entity.User;
import com.gateway.project.authservice.exception.AuthException;
import com.gateway.project.authservice.repository.UserRepository;
import com.gateway.project.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository       userRepository;
    private final RefreshTokenService  refreshTokenService;
    private final JwtService           jwtService;
    private final PasswordEncoder      passwordEncoder;

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException.ConflictException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : User.Role.DEVELOPER)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: {} [{}]", user.getEmail(), user.getRole());
        return buildResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException.UnauthorizedException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new AuthException.UnauthorizedException("Account is disabled. Contact support.");
        }
        if (!user.isAccountNonLocked()) {
            throw new AuthException.UnauthorizedException("Account is locked. Contact support.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException.UnauthorizedException("Invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildResponse(user);
    }

    // ── Refresh access token ──────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken newToken = refreshTokenService.rotate(refreshTokenValue);
        User user = newToken.getUser();

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(newToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    // ── Logout (single device) ────────────────────────────────────────────────

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenService.revoke(refreshTokenValue);
        log.info("Refresh token revoked (single-device logout)");
    }

    // ── Logout all devices ────────────────────────────────────────────────────

    @Transactional
    public void logoutAll(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException.NotFoundException("User", userId));
        refreshTokenService.revokeAll(user);
        log.info("All sessions revoked for user: {}", user.getEmail());
    }

    // ── Change password ───────────────────────────────────────────────────────

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException.NotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthException.UnauthorizedException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAll(user);   // force re-login everywhere
        log.info("Password changed for user: {}. All sessions revoked.", user.getEmail());
    }

    // ── Token validation (used by API Gateway) ────────────────────────────────

    public TokenValidationResponse validateToken(String token) {
        try {
            if (!jwtService.isValid(token)) {
                return TokenValidationResponse.builder().valid(false).build();
            }
            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(jwtService.extractUserId(token))
                    .email(jwtService.extractEmail(token))
                    .role(jwtService.extractRole(token))
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder().valid(false).build();
        }
    }

    // ── User lookup (called by other services via Feign) ──────────────────────

    @Transactional(readOnly = true)
    public UserSummary getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toSummary)
                .orElseThrow(() -> new AuthException.NotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getUsersByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream().map(this::toSummary).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse buildResponse(User user) {
        RefreshToken refreshToken = refreshTokenService.create(user);
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    private UserSummary toSummary(User u) {
        return UserSummary.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .role(u.getRole().name())
                .enabled(u.isEnabled())
                .build();
    }
}
