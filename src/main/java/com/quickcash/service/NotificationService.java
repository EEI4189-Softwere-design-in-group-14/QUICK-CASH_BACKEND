
package com.quickcash.service;

import com.quickcash.dto.NotificationResponse;

import com.quickcash.entity.Notification;
import com.quickcash.entity.NotificationType;
import com.quickcash.entity.User;

import com.quickcash.repository.NotificationRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // CREATE A NOTIFICATION

    @Transactional
    public void send(
            User recipient,
            NotificationType type,
            String title,
            String message
    ) {

        Notification notification = new Notification();

        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);

        notificationRepository.save(notification);
    }

    // GET CURRENT USER'S NOTIFICATIONS

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(
            String email
    ) {

        User user = findUser(email);

        return notificationRepository
                .findByRecipient_UserIdOrderByCreatedAtDesc(
                        user.getUserId()
                )
                .stream()
                .map(NotificationResponse::new)
                .toList();
    }

    // MARK NOTIFICATION AS READ

    @Transactional
    public NotificationResponse markAsRead(
            Long notificationId,
            String email
    ) {

        User user = findUser(email);

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Notification not found"
                        )
                );

        // Prevent access to another user's notification.
        if (!notification.getRecipient()
                .getUserId()
                .equals(user.getUserId())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Notification not found"
            );
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(
                    LocalDateTime.now()
            );
        }

        Notification saved =
                notificationRepository.save(notification);

        return new NotificationResponse(saved);
    }

    private User findUser(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );
    }
}
