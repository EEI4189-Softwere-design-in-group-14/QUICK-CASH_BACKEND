
package com.quickcash.controller;

import com.quickcash.dto.ManagerSmsNotificationResponse;
import com.quickcash.entity.SmsNotificationStatus;
import com.quickcash.service.ManagerSmsNotificationService;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/sms-notifications")
@PreAuthorize("hasAnyRole('MANAGER','OWNER')")
public class ManagerSmsNotificationController {

    private final ManagerSmsNotificationService service;

    public ManagerSmsNotificationController(
            ManagerSmsNotificationService service
    ) {
        this.service = service;
    }

    @GetMapping
    public Page<ManagerSmsNotificationResponse> getNotifications(
            @RequestParam(required = false)
            SmsNotificationStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return service.getNotifications(status, page, size);
    }
}
