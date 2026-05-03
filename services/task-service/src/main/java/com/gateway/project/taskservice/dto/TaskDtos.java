package com.gateway.project.taskservice.dto;

import com.gateway.project.taskservice.entity.Task;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskDtos {

    // ── Requests ──────────────────────────────────────────────────────────────

    @Data
    public static class CreateTaskRequest {

        @NotBlank(message = "Title is required")
        @Size(min = 2, max = 200)
        private String title;

        @Size(max = 2000)
        private String description;

        @NotNull(message = "Project ID is required")
        private Long projectId;

        private Long assigneeId;

        private Task.Type type = Task.Type.TASK;

        private Task.Priority priority = Task.Priority.MEDIUM;

        @Min(1) @Max(100)
        private Integer storyPoints;

        private LocalDate dueDate;
    }

    @Data
    public static class UpdateTaskRequest {

        @Size(min = 2, max = 200)
        private String title;

        @Size(max = 2000)
        private String description;

        private Long assigneeId;

        private Task.Type type;

        private Task.Status status;

        private Task.Priority priority;

        @Min(1) @Max(100)
        private Integer storyPoints;

        private LocalDate dueDate;

        private Long sprintId;
    }

    @Data
    public static class UpdateStatusRequest {
        @NotNull(message = "Status is required")
        private Task.Status status;
    }

    @Data
    public static class AssignTaskRequest {
        private Long assigneeId; // null = unassign
    }

    // ── Responses ─────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TaskResponse {
        private Long id;
        private String title;
        private String description;
        private Long projectId;
        private Long reporterId;
        private String reporterName;
        private Long assigneeId;
        private String assigneeName;
        private Long sprintId;
        private String type;
        private String status;
        private String priority;
        private Integer storyPoints;
        private LocalDate dueDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
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
