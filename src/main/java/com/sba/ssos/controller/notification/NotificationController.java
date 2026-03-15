package com.sba.ssos.controller.notification;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.notification.UserNotificationResponse;
import com.sba.ssos.service.NotificationService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.NOTIFICATIONS)
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Current-user notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final LocaleUtils localeUtils;

    @GetMapping
    public ResponseGeneral<List<UserNotificationResponse>> getCurrentUserNotifications() {
        List<UserNotificationResponse> data = notificationService.getCurrentUserNotifications();
        return ResponseGeneral.ofSuccess(localeUtils.get("success.notification.fetched"), data);
    }

    @PostMapping("/{userNotificationId}/read")
    public ResponseGeneral<Void> markAsRead(@PathVariable UUID userNotificationId) {
        notificationService.markAsRead(userNotificationId);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.notification.read"), null);
    }
}

