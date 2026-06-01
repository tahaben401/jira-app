package com.gateway.project.notificationservice.controller;

import com.gateway.project.notificationservice.dto.NotificationDtos.*;
import com.gateway.project.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    @Value("${internal.service-token}")
    private String internalServiceToken;

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String token) {

        boolean internal = internalServiceToken != null && internalServiceToken.equals(token);
        NotificationResponse response = notificationService.createNotification(request, userId, internal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Notification created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(notificationService.getById(id, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(notificationService.getByUser(userId)));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnread(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(notificationService.getUnreadByUser(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> updateRead(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReadRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Read status updated", notificationService.updateRead(id, request, userId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @RequestHeader("X-User-Id") Long userId) {

        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Notification deleted", null));
    }
}
