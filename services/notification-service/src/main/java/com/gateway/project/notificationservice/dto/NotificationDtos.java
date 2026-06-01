package com.gateway.project.notificationservice.dto;

import com.gateway.project.notificationservice.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class NotificationDtos {

    // Requests

    @Data
    public static class CreateNotificationRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Title is required")
        @Size(min = 2, max = 200)
        private String title;

        @Size(max = 2000)
        private String message;

        private Notification.Type type = Notification.Type.INFO;

        @Size(max = 500)
        private String link;
    }

    @Data
    public static class UpdateReadRequest {
        @NotNull(message = "Read flag is required")
        private Boolean read;
    }

    // Responses

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationResponse {
        private Long id;
        private Long userId;
        private String userName;
        private String title;
        private String message;
        private String type;
        private String link;
        private boolean read;
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
