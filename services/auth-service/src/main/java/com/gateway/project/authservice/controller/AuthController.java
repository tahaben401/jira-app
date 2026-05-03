package com.gateway.project.authservice.controller;

import com.gateway.project.authservice.dto.AuthDtos.*;
import com.gateway.project.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Public ────────────────────────────────────────────────────────────────


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully", data));
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }


    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authService.refresh(request.getRefreshToken())));
    }

    // ── Authenticated ─────────────────────────────────────────────────────────


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }


    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @RequestHeader("X-User-Id") Long userId) {

        authService.logoutAll(userId);
        return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices", null));
    }


    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed. Please log in again.", null));
    }

    // ── Internal (API Gateway + other services) ───────────────────────────────

    /**
     * Validate an access token — called by the API Gateway filter.
     * GET /api/v1/auth/validate?token=...
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validate(
            @RequestParam String token) {

        return ResponseEntity.ok(ApiResponse.ok(authService.validateToken(token)));
    }

    /**
     * Get a user's public info by ID — called by other services via Feign.
     * GET /api/v1/auth/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserSummary>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getUserById(id)));
    }

    /**
     * Batch user lookup by list of IDs.
     * POST /api/v1/auth/users/batch
     */
    @PostMapping("/users/batch")
    public ResponseEntity<ApiResponse<List<UserSummary>>> getUsersByIds(
            @RequestBody List<Long> ids) {

        return ResponseEntity.ok(ApiResponse.ok(authService.getUsersByIds(ids)));
    }

    /**
     * Get the currently authenticated user's profile.
     * GET /api/v1/auth/me
     * Header: X-User-Id (injected by API Gateway)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummary>> getMe(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(authService.getUserById(userId)));
    }
}
