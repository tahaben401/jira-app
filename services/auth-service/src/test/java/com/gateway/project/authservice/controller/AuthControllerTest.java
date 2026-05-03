package com.gateway.project.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.project.authservice.config.GlobalExceptionHandler;
import com.gateway.project.authservice.config.SecurityConfig;
import com.gateway.project.authservice.dto.AuthDtos.*;
import com.gateway.project.authservice.exception.AuthException;
import com.gateway.project.authservice.security.JwtAuthenticationFilter;
import com.gateway.project.authservice.security.JwtService;
import com.gateway.project.authservice.security.UserDetailsServiceImpl;
import com.gateway.project.authservice.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  mapper;

    @MockBean AuthService            authService;
    @MockBean JwtService             jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private static final String BASE = "/api/v1/auth";

    // ── POST /register ────────────────────────────────────────────────────────

    @Nested @DisplayName("POST /register")
    class Register {

        @Test @DisplayName("201 with token pair on valid request")
        void success() throws Exception {
            var req  = Map.of("email","alice@test.com","password","Password1!","fullName","Alice");
            var resp = authResponse("access-jwt", "refresh-token", 1L, "alice@test.com");

            when(authService.register(any())).thenReturn(resp);

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-jwt"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test @DisplayName("400 when email is invalid")
        void invalidEmail() throws Exception {
            var req = Map.of("email","not-an-email","password","Password1!","fullName","Alice");

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test @DisplayName("400 when password is too short")
        void shortPassword() throws Exception {
            var req = Map.of("email","alice@test.com","password","abc","fullName","Alice");

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("409 when email already registered")
        void duplicateEmail() throws Exception {
            var req = Map.of("email","alice@test.com","password","Password1!","fullName","Alice");
            when(authService.register(any()))
                    .thenThrow(new AuthException.ConflictException("Email already registered"));

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }
    }

    // ── POST /login ───────────────────────────────────────────────────────────

    @Nested @DisplayName("POST /login")
    class Login {

        @Test @DisplayName("200 with token pair on valid credentials")
        void success() throws Exception {
            var req  = Map.of("email","alice@test.com","password","Password1!");
            var resp = authResponse("jwt-access", "jwt-refresh", 1L, "alice@test.com");

            when(authService.login(any())).thenReturn(resp);

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-access"))
                    .andExpect(jsonPath("$.data.userId").value(1));
        }

        @Test @DisplayName("401 on wrong credentials")
        void wrongCredentials() throws Exception {
            var req = Map.of("email","alice@test.com","password","wrong");
            when(authService.login(any()))
                    .thenThrow(new AuthException.UnauthorizedException("Invalid email or password"));

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test @DisplayName("400 when email field is missing")
        void missingEmail() throws Exception {
            var req = Map.of("password","Password1!");

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── POST /refresh ─────────────────────────────────────────────────────────

    @Nested @DisplayName("POST /refresh")
    class Refresh {

        @Test @DisplayName("200 with new token pair")
        void success() throws Exception {
            var req  = Map.of("refreshToken","valid-rt");
            var resp = authResponse("new-access", "new-refresh", 1L, "alice@test.com");

            when(authService.refresh("valid-rt")).thenReturn(resp);

            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
        }

        @Test @DisplayName("401 when refresh token is expired or revoked")
        void invalidToken() throws Exception {
            var req = Map.of("refreshToken","bad-rt");
            when(authService.refresh("bad-rt"))
                    .thenThrow(new AuthException.UnauthorizedException("Refresh token has expired"));

            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("400 when refreshToken field is blank")
        void blankToken() throws Exception {
            var req = Map.of("refreshToken","");

            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── POST /logout ──────────────────────────────────────────────────────────

    @Nested @DisplayName("POST /logout")
    class Logout {

        @Test @DisplayName("200 on successful logout")
        void success() throws Exception {
            var req = Map.of("refreshToken","my-rt");
            doNothing().when(authService).logout("my-rt");

            mockMvc.perform(post(BASE + "/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── POST /logout-all ──────────────────────────────────────────────────────

    @Nested @DisplayName("POST /logout-all")
    class LogoutAll {

        @Test @DisplayName("200 when X-User-Id header is provided")
        void success() throws Exception {
            doNothing().when(authService).logoutAll(1L);

            mockMvc.perform(post(BASE + "/logout-all")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── GET /validate ─────────────────────────────────────────────────────────

    @Nested @DisplayName("GET /validate")
    class Validate {

        @Test @DisplayName("200 valid=true for a good token")
        void validToken() throws Exception {
            var tvr = TokenValidationResponse.builder()
                    .valid(true).userId(1L).email("alice@test.com").role("DEVELOPER").build();

            when(authService.validateToken("good-jwt")).thenReturn(tvr);

            mockMvc.perform(get(BASE + "/validate").param("token","good-jwt"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.valid").value(true))
                    .andExpect(jsonPath("$.data.email").value("alice@test.com"))
                    .andExpect(jsonPath("$.data.role").value("DEVELOPER"));
        }

        @Test @DisplayName("200 valid=false for a bad token")
        void invalidToken() throws Exception {
            var tvr = TokenValidationResponse.builder().valid(false).build();
            when(authService.validateToken("bad-jwt")).thenReturn(tvr);

            mockMvc.perform(get(BASE + "/validate").param("token","bad-jwt"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.valid").value(false));
        }
    }

    // ── GET /users/{id} ───────────────────────────────────────────────────────

    @Nested @DisplayName("GET /users/{id}")
    class GetUserById {

        @Test @DisplayName("200 with user summary")
        void success() throws Exception {
            var summary = UserSummary.builder()
                    .id(1L).email("alice@test.com").fullName("Alice")
                    .role("DEVELOPER").enabled(true).build();

            when(authService.getUserById(1L)).thenReturn(summary);

            mockMvc.perform(get(BASE + "/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value("alice@test.com"));
        }

        @Test @DisplayName("404 when user not found")
        void notFound() throws Exception {
            when(authService.getUserById(99L))
                    .thenThrow(new AuthException.NotFoundException("User", 99L));

            mockMvc.perform(get(BASE + "/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ── GET /me ───────────────────────────────────────────────────────────────

    @Test @DisplayName("GET /me — 200 with current user profile")
    void getMe() throws Exception {
        var summary = UserSummary.builder()
                .id(2L).email("bob@test.com").fullName("Bob")
                .role("QA").enabled(true).build();

        when(authService.getUserById(2L)).thenReturn(summary);

        mockMvc.perform(get(BASE + "/me").header("X-User-Id","2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("bob@test.com"))
                .andExpect(jsonPath("$.data.role").value("QA"));
    }

    // ── POST /users/batch ─────────────────────────────────────────────────────

    @Test @DisplayName("POST /users/batch — 200 with list of user summaries")
    void getUsersByIds() throws Exception {
        var summaries = List.of(
                UserSummary.builder().id(1L).email("a@test.com").role("DEVELOPER").build(),
                UserSummary.builder().id(2L).email("b@test.com").role("QA").build()
        );
        when(authService.getUsersByIds(List.of(1L, 2L))).thenReturn(summaries);

        mockMvc.perform(post(BASE + "/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].email").value("a@test.com"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse authResponse(String access, String refresh, Long userId, String email) {
        return AuthResponse.builder()
                .accessToken(access).refreshToken(refresh)
                .tokenType("Bearer").expiresIn(900L)
                .userId(userId).email(email)
                .fullName("Test User").role("DEVELOPER")
                .build();
    }

    // static import helper to avoid full java.util.Map import issues in older test runners
    private static java.util.Map<String,String> Map(String... kv) {
        var m = new java.util.HashMap<String,String>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i+1]);
        return m;
    }
}
