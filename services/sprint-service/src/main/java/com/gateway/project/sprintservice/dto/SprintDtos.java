package com.gateway.project.sprintservice.dto;

import com.gateway.project.sprintservice.entity.Sprint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SprintDtos {

    // Requests

    @Data
    public static class CreateSprintRequest {
        @NotNull(message = "Project ID is required")
        private Long projectId;

        @NotBlank(message = "Sprint name is required")
        @Size(min = 2, max = 120)
        private String name;

        @Size(max = 1000)
        private String goal;

        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class UpdateSprintRequest {
        @Size(min = 2, max = 120)
        private String name;

        @Size(max = 1000)
        private String goal;

        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class UpdateStatusRequest {
        @NotNull(message = "Status is required")
        private Sprint.Status status;
    }

    // Responses

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintResponse {
        private Long id;
        private Long projectId;
        private String name;
        private String goal;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long createdBy;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // Generic wrapper

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
