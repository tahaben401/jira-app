package com.gateway.project.notificationservice.service;

import com.gateway.project.notificationservice.client.AuthServiceClient;
import com.gateway.project.notificationservice.dto.NotificationDtos.*;
import com.gateway.project.notificationservice.entity.Notification;
import com.gateway.project.notificationservice.exception.NotificationException;
import com.gateway.project.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request, Long actorUserId, boolean internal) {
        if (!internal) {
            if (actorUserId == null || !actorUserId.equals(request.getUserId())) {
                throw new NotificationException.UnauthorizedException("You can only create notifications for yourself");
            }
        }

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType() != null ? request.getType() : Notification.Type.INFO)
                .link(request.getLink())
                .read(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification {} created for user {}", notification.getId(), notification.getUserId());
        return enrich(notification);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long notificationId, Long userId) {
        Notification notification = findNotification(notificationId);
        assertOwner(notification, userId);
        return enrich(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByUser(Long userId) {
        return enrichAll(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadByUser(Long userId) {
        return enrichAll(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId));
    }

    @Transactional
    public NotificationResponse updateRead(Long notificationId, UpdateReadRequest request, Long userId) {
        Notification notification = findNotification(notificationId);
        assertOwner(notification, userId);
        notification.setRead(request.getRead());
        return enrich(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        if (unread.isEmpty()) return;
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = findNotification(notificationId);
        assertOwner(notification, userId);
        notificationRepository.delete(notification);
        log.info("Notification {} deleted by user {}", notificationId, userId);
    }

    private Notification findNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationException.NotFoundException("Notification", id));
    }

    private void assertOwner(Notification notification, Long userId) {
        if (userId == null || !notification.getUserId().equals(userId)) {
            throw new NotificationException.UnauthorizedException("You don't have access to this notification");
        }
    }

    private NotificationResponse enrich(Notification notification) {
        return enrichAll(List.of(notification)).get(0);
    }

    private List<NotificationResponse> enrichAll(List<Notification> notifications) {
        if (notifications.isEmpty()) return List.of();

        List<Long> userIds = notifications.stream().map(Notification::getUserId).distinct().toList();
        Map<Long, String> userNames = fetchUserNames(userIds);

        return notifications.stream().map(n -> NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .userName(userNames.getOrDefault(n.getUserId(), "Unknown"))
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType().name())
                .link(n.getLink())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build()).toList();
    }

    private Map<Long, String> fetchUserNames(List<Long> userIds) {
        try {
            var resp = authServiceClient.getUsersByIds(userIds);
            return resp.getData().stream()
                    .collect(Collectors.toMap(
                            AuthServiceClient.UserSummary::getId,
                            AuthServiceClient.UserSummary::getFullName
                    ));
        } catch (Exception e) {
            log.warn("Could not fetch user info from auth-service: {}", e.getMessage());
            return Map.of();
        }
    }
}
