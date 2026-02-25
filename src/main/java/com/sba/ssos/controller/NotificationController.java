package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.notification.UserNotificationResponse;
import com.sba.ssos.service.NotificationService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
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

