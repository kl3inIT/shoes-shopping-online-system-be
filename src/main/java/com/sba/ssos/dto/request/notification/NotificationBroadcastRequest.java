package com.sba.ssos.dto.request.notification;

import com.sba.ssos.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationBroadcastRequest(
        @Size(max = 255, message = "validation.notification.title.size")
        @NotBlank String title,
        @Size(max = 2000, message = "validation.notification.message.size")
        @NotBlank String message,
        @NotNull NotificationType type
) {
}

