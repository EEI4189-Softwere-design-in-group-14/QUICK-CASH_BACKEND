
package com.quickcash.repository;

import com.quickcash.entity.Notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
    findByRecipient_UserIdOrderByCreatedAtDesc(
            Long userId
    );

}
