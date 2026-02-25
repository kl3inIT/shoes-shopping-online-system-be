package com.sba.ssos.service;

import com.sba.ssos.dto.request.notification.NotificationBroadcastRequest;
import com.sba.ssos.dto.response.notification.NotificationResponse;
import com.sba.ssos.dto.response.notification.UserNotificationResponse;
import com.sba.ssos.entity.Notification;
import com.sba.ssos.entity.User;
import com.sba.ssos.entity.UserNotification;
import com.sba.ssos.enums.NotificationType;
import com.sba.ssos.repository.NotificationRepository;
import com.sba.ssos.repository.UserNotificationRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.security.AuthorizedUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Transactional
    public NotificationResponse broadcast(NotificationBroadcastRequest request) {
        Notification notification = Notification.builder()
                .title(request.title())
                .message(request.message())
                .type(request.type())
                .build();

        Notification saved = notificationRepository.save(notification);

        // Gắn thông báo này cho tất cả người dùng
        List<User> users = userRepository.findAll();
        List<UserNotification> userNotifications = users.stream()
                .map(user -> UserNotification.builder()
                        .user(user)
                        .notification(saved)
                        .read(false)
                        .build())
                .collect(Collectors.toList());

        userNotificationRepository.saveAll(userNotifications);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(NotificationType type) {
        List<Notification> notifications;
        if (type != null) {
            notifications = notificationRepository.findByType(type);
        } else {
            notifications = notificationRepository.findAll();
        }
        // Mới nhất lên trước
        return notifications.stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getCurrentUserNotifications() {
        User currentUser = getCurrentUserEntity();
        List<UserNotification> userNotifications = userNotificationRepository.findByUser(currentUser);

        return userNotifications.stream()
                .sorted(Comparator.comparing(
                        (UserNotification un) -> un.getNotification().getCreatedAt()
                ).reversed())
                .map(this::toUserNotificationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(UUID userNotificationId) {
        UserNotification userNotification = userNotificationRepository.findById(userNotificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        // Đảm bảo chỉ được sửa thông báo của chính người dùng hiện tại
        User currentUser = getCurrentUserEntity();
        if (!userNotification.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Cannot modify notification of another user");
        }

        if (!userNotification.isRead()) {
            userNotification.setRead(true);
            userNotificationRepository.save(userNotification);
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getCreatedAt()
        );
    }

    private UserNotificationResponse toUserNotificationResponse(UserNotification userNotification) {
        Notification notification = userNotification.getNotification();
        return new UserNotificationResponse(
                userNotification.getId(),
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                userNotification.isRead(),
                notification.getCreatedAt()
        );
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthorizedUserDetails userDetails)) {
            throw new IllegalStateException("Unexpected principal type");
        }

        UUID keycloakId = userDetails.userId();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}

