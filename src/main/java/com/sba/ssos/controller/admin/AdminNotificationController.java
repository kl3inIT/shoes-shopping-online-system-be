package com.sba.ssos.controller.admin;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.notification.NotificationBroadcastRequest;
import com.sba.ssos.dto.response.notification.NotificationResponse;
import com.sba.ssos.enums.NotificationType;
import com.sba.ssos.service.NotificationService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final LocaleUtils localeUtils;

    @PostMapping("/broadcast")
    public ResponseGeneral<NotificationResponse> broadcast(
            @Valid @RequestBody NotificationBroadcastRequest request) {
        NotificationResponse data = notificationService.broadcast(request);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.notification.broadcast"), data);
    }

    @GetMapping
    public ResponseGeneral<List<NotificationResponse>> getNotifications(
            @RequestParam(name = "type", required = false) NotificationType type) {
        List<NotificationResponse> data = notificationService.getNotifications(type);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.notification.fetched"), data);
    }
}

