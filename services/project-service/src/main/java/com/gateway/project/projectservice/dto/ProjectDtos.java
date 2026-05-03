package com.gateway.project.projectservice.dto;

import com.gateway.project.projectservice.entity.Project;
import com.gateway.project.projectservice.entity.ProjectMember;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ProjectDtos {

    // ── Requests ──────────────────────────────────────────────────────────────

    @Data
    public static class CreateProjectRequest {
        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 100)
        private String name;

        @Size(max = 500)
        private String description;
    }

    @Data
    public static class UpdateProjectRequest {
        @Size(min = 2, max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        private Project.Status status;
    }

    @Data
    public static class InviteMemberRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Role is required")
        private ProjectMember.Role role;
    }

    @Data
    public static class UpdateMemberRoleRequest {
        @NotNull(message = "Role is required")
        private ProjectMember.Role role;
    }

    // ── Responses ─────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProjectResponse {
        private Long id;
        private String name;
        private String description;
        private Long ownerId;
        private String status;
        private int memberCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProjectDetailResponse {
        private Long id;
        private String name;
        private String description;
        private Long ownerId;
        private String status;
        private List<MemberResponse> members;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MemberResponse {
        private Long id;
        private Long userId;
        private String fullName;
        private String email;
        private String role;
        private LocalDateTime joinedAt;
    }

    // ── Generic wrapper ───────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
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
