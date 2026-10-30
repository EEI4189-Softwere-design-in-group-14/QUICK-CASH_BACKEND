
package com.quickcash.repository;

import com.quickcash.entity.InstallmentReminderLog;
import com.quickcash.entity.NotificationType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentReminderLogRepository
        extends JpaRepository<InstallmentReminderLog, Long> {

    boolean existsByInstallmentInstallmentIdAndReminderType(
            Long installmentId,
            NotificationType reminderType
    );
}
