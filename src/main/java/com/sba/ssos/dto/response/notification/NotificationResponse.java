package com.sba.ssos.dto.response.notification;

import com.sba.ssos.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationType type,
        Instant createdAt
) {
}

