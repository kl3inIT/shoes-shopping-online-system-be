package com.sba.ssos.dto.response.notification;

import com.sba.ssos.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record UserNotificationResponse(
        UUID id,
        UUID notificationId,
        String title,
        String message,
        NotificationType type,
        boolean isRead,
        Instant createdAt
) {
}

