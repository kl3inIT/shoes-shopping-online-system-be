package com.sba.ssos.service;

import com.sba.ssos.dto.request.notification.NotificationBroadcastRequest;
import com.sba.ssos.dto.response.notification.NotificationResponse;
import com.sba.ssos.dto.response.notification.UserNotificationResponse;
import com.sba.ssos.entity.Notification;
import com.sba.ssos.entity.UserNotification;
import com.sba.ssos.enums.NotificationType;
import com.sba.ssos.exception.base.ForbiddenException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.NotificationRepository;
import com.sba.ssos.repository.UserNotificationRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.service.user.AuthenticatedUserService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final UserNotificationRepository userNotificationRepository;
  private final AuthenticatedUserService authenticatedUserService;

  @Transactional
  public NotificationResponse broadcast(NotificationBroadcastRequest request) {
    log.info("Broadcasting notification of type {}", request.type());
    Notification saved =
        notificationRepository.save(
            Notification.builder()
                .title(request.title())
                .message(request.message())
                .type(request.type())
                .build());

    List<UserNotification> userNotifications =
        userRepository.findAll().stream()
            .map(
                user ->
                    UserNotification.builder()
                        .user(user)
                        .notification(saved)
                        .read(false)
                        .build())
            .collect(Collectors.toList());

    userNotificationRepository.saveAll(userNotifications);
    log.info("Broadcast notification {} to {} users", saved.getId(), userNotifications.size());
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> getNotifications(NotificationType type) {
    List<Notification> notifications =
        type != null ? notificationRepository.findByType(type) : notificationRepository.findAll();

    return notifications.stream()
        .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<UserNotificationResponse> getCurrentUserNotifications() {
    var currentUser = authenticatedUserService.getCurrentUserEntity();

    return userNotificationRepository.findByUser(currentUser).stream()
        .sorted(
            Comparator.comparing(
                    (UserNotification userNotification) ->
                        userNotification.getNotification().getCreatedAt())
                .reversed())
        .map(this::toUserNotificationResponse)
        .toList();
  }

  @Transactional
  public void markAsRead(UUID userNotificationId) {
    UserNotification userNotification =
        userNotificationRepository
            .findById(userNotificationId)
            .orElseThrow(() -> new NotFoundException("Notification", userNotificationId));

    var currentUser = authenticatedUserService.getCurrentUserEntity();
    if (!userNotification.getUser().getId().equals(currentUser.getId())) {
      log.warn(
          "User {} attempted to mark notification {} that does not belong to them",
          currentUser.getId(),
          userNotificationId);
      throw new ForbiddenException("error.auth.forbidden");
    }

    if (!userNotification.isRead()) {
      userNotification.setRead(true);
      userNotificationRepository.save(userNotification);
      log.info("Marked notification {} as read for user {}", userNotificationId, currentUser.getId());
    }
  }

  private NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getTitle(),
        notification.getMessage(),
        notification.getType(),
        notification.getCreatedAt());
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
        notification.getCreatedAt());
  }
}
