
package com.quickcash.dto;

import com.quickcash.entity.Notification;
import com.quickcash.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(

        Long notificationId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt

) {

    public NotificationResponse(Notification notification) {
        this(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
