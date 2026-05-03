package com.gateway.project.projectservice.client;

import lombok.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "auth-service", path = "/api/v1/auth")
public interface AuthServiceClient {

    @PostMapping("/users/batch")
    ApiResponse<List<UserSummary>> getUsersByIds(@RequestBody List<Long> ids);

    // ── DTOs matching auth-service response ──────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    class ApiResponse<T> {
        private boolean success;
        private T data;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    class UserSummary {
        private Long id;
        private String email;
        private String fullName;
        private String role;
        private boolean enabled;
    }
}
