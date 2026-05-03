package com.gateway.project.authservice.service;

import com.gateway.project.authservice.entity.RefreshToken;
import com.gateway.project.authservice.entity.User;
import com.gateway.project.authservice.exception.AuthException;
import com.gateway.project.authservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repo;
    @InjectMocks private RefreshTokenService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenExpiration", 604800000L);
        user = User.builder()
                .id(1L).email("alice@test.com").password("hashed")
                .fullName("Alice").role(User.Role.DEVELOPER)
                .enabled(true).accountNonLocked(true).build();
    }

    // ── create() ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("create()")
    class Create {

        @Test @DisplayName("saves and returns a new token")
        void success() {
            when(repo.countByUserAndRevokedFalse(user)).thenReturn(0L);
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken token = service.create(user);

            assertThat(token.getToken()).isNotBlank();
            assertThat(token.isRevoked()).isFalse();
            assertThat(token.getExpiresAt()).isAfter(Instant.now());
            assertThat(token.getUser()).isEqualTo(user);
            verify(repo).save(any(RefreshToken.class));
        }

        @Test @DisplayName("revokes all sessions when MAX_ACTIVE_SESSIONS reached")
        void revokesWhenMaxReached() {
            when(repo.countByUserAndRevokedFalse(user)).thenReturn(5L);
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.create(user);

            verify(repo).revokeAllByUser(user);
        }

        @Test @DisplayName("does not revoke when under session limit")
        void noRevokeUnderLimit() {
            when(repo.countByUserAndRevokedFalse(user)).thenReturn(2L);
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.create(user);

            verify(repo, never()).revokeAllByUser(any());
        }
    }

    // ── validate() ───────────────────────────────────────────────────────────

    @Nested @DisplayName("validate()")
    class Validate {

        @Test @DisplayName("returns token when valid")
        void success() {
            var token = token("valid-token", false, Instant.now().plusSeconds(3600));
            when(repo.findByToken("valid-token")).thenReturn(Optional.of(token));

            RefreshToken result = service.validate("valid-token");
            assertThat(result.getToken()).isEqualTo("valid-token");
        }

        @Test @DisplayName("throws when token not found")
        void notFound() {
            when(repo.findByToken("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.validate("ghost"))
                    .isInstanceOf(AuthException.UnauthorizedException.class)
                    .hasMessageContaining("not found");
        }

        @Test @DisplayName("throws and revokes all sessions on revoked token reuse")
        void revokedReuse() {
            var token = token("revoked-token", true, Instant.now().plusSeconds(3600));
            when(repo.findByToken("revoked-token")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.validate("revoked-token"))
                    .isInstanceOf(AuthException.UnauthorizedException.class)
                    .hasMessageContaining("reuse");

            verify(repo).revokeAllByUser(user);
        }

        @Test @DisplayName("throws when token is expired")
        void expired() {
            var token = token("expired-token", false, Instant.now().minusSeconds(10));
            when(repo.findByToken("expired-token")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.validate("expired-token"))
                    .isInstanceOf(AuthException.UnauthorizedException.class)
                    .hasMessageContaining("expired");
        }
    }

    // ── rotate() ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("rotate()")
    class Rotate {

        @Test @DisplayName("revokes old token and creates a new one")
        void success() {
            var old = token("old-token", false, Instant.now().plusSeconds(3600));
            when(repo.findByToken("old-token")).thenReturn(Optional.of(old));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(repo.countByUserAndRevokedFalse(user)).thenReturn(1L);

            RefreshToken newToken = service.rotate("old-token");

            assertThat(old.isRevoked()).isTrue();
            assertThat(newToken.getToken()).isNotEqualTo("old-token");
            assertThat(newToken.isRevoked()).isFalse();
        }
    }

    // ── revoke() ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("revoke()")
    class Revoke {

        @Test @DisplayName("marks token as revoked")
        void success() {
            var token = token("my-token", false, Instant.now().plusSeconds(3600));
            when(repo.findByToken("my-token")).thenReturn(Optional.of(token));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.revoke("my-token");

            assertThat(token.isRevoked()).isTrue();
            verify(repo).save(token);
        }

        @Test @DisplayName("does nothing when token not found")
        void notFound() {
            when(repo.findByToken("ghost")).thenReturn(Optional.empty());
            assertThatNoException().isThrownBy(() -> service.revoke("ghost"));
            verify(repo, never()).save(any());
        }
    }

    // ── revokeAll() ───────────────────────────────────────────────────────────

    @Test @DisplayName("revokeAll() delegates to repository")
    void revokeAll() {
        service.revokeAll(user);
        verify(repo).revokeAllByUser(user);
    }

    // ── cleanupExpired() ──────────────────────────────────────────────────────

    @Test @DisplayName("cleanupExpired() calls repository delete")
    void cleanupExpired() {
        service.cleanupExpired();
        verify(repo).deleteExpiredAndRevoked(any(Instant.class));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RefreshToken token(String value, boolean revoked, Instant expiresAt) {
        return RefreshToken.builder()
                .id(1L).token(value).user(user)
                .expiresAt(expiresAt).revoked(revoked).build();
    }
}
