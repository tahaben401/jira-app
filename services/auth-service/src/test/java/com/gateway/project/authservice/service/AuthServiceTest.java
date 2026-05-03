package com.gateway.project.authservice.service;

import com.gateway.project.authservice.dto.AuthDtos.*;
import com.gateway.project.authservice.entity.RefreshToken;
import com.gateway.project.authservice.entity.User;
import com.gateway.project.authservice.exception.AuthException;
import com.gateway.project.authservice.repository.UserRepository;
import com.gateway.project.authservice.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository      userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtService          jwtService;
    @Mock private PasswordEncoder     passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // ── Register ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("register()")
    class Register {

        @Test @DisplayName("creates user and returns token pair")
        void success() {
            var req = registerRequest("alice@test.com", "Password1!", "Alice");
            var saved = user(1L, "alice@test.com", User.Role.DEVELOPER);
            var rt = refreshToken("rt-value", saved);

            when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(saved);
            when(jwtService.generateAccessToken(saved)).thenReturn("access-jwt");
            when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
            when(refreshTokenService.create(saved)).thenReturn(rt);

            AuthResponse res = authService.register(req);

            assertThat(res.getAccessToken()).isEqualTo("access-jwt");
            assertThat(res.getRefreshToken()).isEqualTo("rt-value");
            assertThat(res.getTokenType()).isEqualTo("Bearer");
            verify(userRepository).save(any(User.class));
        }

        @Test @DisplayName("throws ConflictException when email taken")
        void duplicateEmail() {
            var req = registerRequest("taken@test.com", "Password1!", "Bob");
            when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(AuthException.ConflictException.class)
                    .hasMessageContaining("already registered");
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Nested @DisplayName("login()")
    class Login {

        @Test @DisplayName("returns token pair on valid credentials")
        void success() {
            var req = loginRequest("bob@test.com", "correct");
            var u   = user(2L, "bob@test.com", User.Role.QA);
            var rt  = refreshToken("rt-bob", u);

            when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("correct", u.getPassword())).thenReturn(true);
            when(jwtService.generateAccessToken(u)).thenReturn("jwt-bob");
            when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
            when(refreshTokenService.create(u)).thenReturn(rt);

            AuthResponse res = authService.login(req);
            assertThat(res.getUserId()).isEqualTo(2L);
            assertThat(res.getAccessToken()).isEqualTo("jwt-bob");
        }

        @Test @DisplayName("throws UnauthorizedException on wrong password")
        void wrongPassword() {
            var req = loginRequest("bob@test.com", "wrong");
            var u   = user(2L, "bob@test.com", User.Role.QA);

            when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("wrong", u.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(AuthException.UnauthorizedException.class);
        }

        @Test @DisplayName("throws UnauthorizedException when user not found")
        void userNotFound() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest("x@x.com", "pass")))
                    .isInstanceOf(AuthException.UnauthorizedException.class);
        }

        @Test @DisplayName("throws UnauthorizedException when account disabled")
        void accountDisabled() {
            var u = user(3L, "dis@test.com", User.Role.DEVELOPER);
            u.setEnabled(false);
            when(userRepository.findByEmail("dis@test.com")).thenReturn(Optional.of(u));

            assertThatThrownBy(() -> authService.login(loginRequest("dis@test.com", "pass")))
                    .isInstanceOf(AuthException.UnauthorizedException.class)
                    .hasMessageContaining("disabled");
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("refresh()")
    class Refresh {

        @Test @DisplayName("rotates token and returns new pair")
        void success() {
            var u  = user(1L, "alice@test.com", User.Role.DEVELOPER);
            var rt = refreshToken("new-rt", u);

            when(refreshTokenService.rotate("old-rt")).thenReturn(rt);
            when(jwtService.generateAccessToken(u)).thenReturn("new-access");
            when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

            AuthResponse res = authService.refresh("old-rt");
            assertThat(res.getAccessToken()).isEqualTo("new-access");
            assertThat(res.getRefreshToken()).isEqualTo("new-rt");
        }
    }

    // ── Change password ───────────────────────────────────────────────────────

    @Nested @DisplayName("changePassword()")
    class ChangePassword {

        @Test @DisplayName("changes password and revokes all sessions")
        void success() {
            var u   = user(1L, "alice@test.com", User.Role.DEVELOPER);
            var req = changePasswordRequest("oldPass", "newPass123");

            when(userRepository.findById(1L)).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("oldPass", u.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newPass123")).thenReturn("newHashed");
            when(userRepository.save(any())).thenReturn(u);

            authService.changePassword(1L, req);

            assertThat(u.getPassword()).isEqualTo("newHashed");
            verify(refreshTokenService).revokeAll(u);
        }

        @Test @DisplayName("throws when current password is wrong")
        void wrongCurrentPassword() {
            var u   = user(1L, "alice@test.com", User.Role.DEVELOPER);
            var req = changePasswordRequest("wrongOld", "newPass123");

            when(userRepository.findById(1L)).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("wrongOld", u.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, req))
                    .isInstanceOf(AuthException.UnauthorizedException.class)
                    .hasMessageContaining("Current password");
        }
    }

    // ── validateToken() ───────────────────────────────────────────────────────

    @Nested @DisplayName("validateToken()")
    class ValidateToken {

        @Test @DisplayName("returns valid=true for good token")
        void valid() {
            when(jwtService.isValid("good")).thenReturn(true);
            when(jwtService.extractUserId("good")).thenReturn(1L);
            when(jwtService.extractEmail("good")).thenReturn("alice@test.com");
            when(jwtService.extractRole("good")).thenReturn("DEVELOPER");

            var res = authService.validateToken("good");
            assertThat(res.isValid()).isTrue();
            assertThat(res.getEmail()).isEqualTo("alice@test.com");
        }

        @Test @DisplayName("returns valid=false for bad token")
        void invalid() {
            when(jwtService.isValid("bad")).thenReturn(false);
            assertThat(authService.validateToken("bad").isValid()).isFalse();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User user(Long id, String email, User.Role role) {
        return User.builder().id(id).email(email).password("hashed")
                .fullName("Test User").role(role).enabled(true).accountNonLocked(true).build();
    }

    private RefreshToken refreshToken(String value, User user) {
        return RefreshToken.builder().id(1L).token(value).user(user)
                .expiresAt(Instant.now().plusSeconds(604800)).revoked(false).build();
    }

    private RegisterRequest registerRequest(String email, String pass, String name) {
        var r = new RegisterRequest(); r.setEmail(email); r.setPassword(pass); r.setFullName(name); return r;
    }

    private LoginRequest loginRequest(String email, String pass) {
        var r = new LoginRequest(); r.setEmail(email); r.setPassword(pass); return r;
    }

    private ChangePasswordRequest changePasswordRequest(String current, String next) {
        var r = new ChangePasswordRequest(); r.setCurrentPassword(current); r.setNewPassword(next); return r;
    }
}
