package com.gateway.project.authservice.dto;

import com.gateway.project.authservice.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;

public class AuthDtos {

    // ── Register ──────────────────────────────────────────────────────────────
    @Data
    public static class RegisterRequest {

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100)
        private String fullName;

        private User.Role role = User.Role.DEVELOPER;
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    @Data
    public static class LoginRequest {

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    // ── Token response (login / register / refresh) ───────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;       // seconds
        private Long userId;
        private String email;
        private String fullName;
        private String role;
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @Data
    public static class LogoutRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    // ── Change password ───────────────────────────────────────────────────────
    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 8, message = "New password must be at least 8 characters")
        private String newPassword;
    }

    // ── Public user info (consumed by other services via Feign) ──────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String email;
        private String fullName;
        private String role;
        private boolean enabled;
    }

    // ── Token validation response (consumed by API Gateway) ──────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenValidationResponse {
        private boolean valid;
        private Long userId;
        private String email;
        private String role;
    }

    // ── Generic API wrapper ───────────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(T data) {
            return ApiResponse.<T>builder().success(true).data(data).build();
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }
}
