package com.sba.ssos.dto.request.notification;

import com.sba.ssos.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationBroadcastRequest(
        @NotBlank String title,
        @NotBlank String message,
        @NotNull NotificationType type
) {
}

