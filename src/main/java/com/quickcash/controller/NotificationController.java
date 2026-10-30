
package com.quickcash.controller;

import com.quickcash.dto.NotificationResponse;
import com.quickcash.service.NotificationService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public List<NotificationResponse> getMyNotifications(
            Principal principal
    ) {

        return notificationService.getMyNotifications(
                principal.getName()
        );
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId,
            Principal principal
    ) {

        return notificationService.markAsRead(
                notificationId,
                principal.getName()
        );
    }
}
