
package com.quickcash.repository;

import com.quickcash.entity.SmsNotification;
import com.quickcash.entity.SmsNotificationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SmsNotificationRepository
        extends JpaRepository<SmsNotification, Long> {

    Optional<SmsNotification> findByPayment_PaymentId(
            Long paymentId
    );

    List<SmsNotification> findByStatusOrderByCreatedAtAsc(
            SmsNotificationStatus status
    );

    // Load a limited batch of pending SMS messages.
    List<SmsNotification> findTop10ByStatusOrderByCreatedAtAsc(
            SmsNotificationStatus status
    );

    // Atomically claim one SMS for processing.
    @Modifying
    @Transactional
    @Query("""
            UPDATE SmsNotification s
            SET s.status = :processing,
                s.attemptCount = s.attemptCount + 1
            WHERE s.smsId = :smsId
              AND s.status = :pending
            """)
    int claimSms(
            @Param("smsId") Long smsId,
            @Param("pending") SmsNotificationStatus pending,
            @Param("processing") SmsNotificationStatus processing
    );

    // Mark gateway-accepted SMS as sent.
    @Modifying
    @Transactional
    @Query("""
            UPDATE SmsNotification s
            SET s.status = :sent,
                s.sentAt = :sentAt,
                s.lastError = null
            WHERE s.smsId = :smsId
              AND s.status = :processing
            """)
    int markSent(
            @Param("smsId") Long smsId,
            @Param("processing") SmsNotificationStatus processing,
            @Param("sent") SmsNotificationStatus sent,
            @Param("sentAt") LocalDateTime sentAt
    );

    // Save a failed sending attempt.
    @Modifying
    @Transactional
    @Query("""
            UPDATE SmsNotification s
            SET s.status = :failed,
                s.lastError = :errorMessage
            WHERE s.smsId = :smsId
              AND s.status = :processing
            """)
    int markFailed(
            @Param("smsId") Long smsId,
            @Param("processing") SmsNotificationStatus processing,
            @Param("failed") SmsNotificationStatus failed,
            @Param("errorMessage") String errorMessage
    );
}
