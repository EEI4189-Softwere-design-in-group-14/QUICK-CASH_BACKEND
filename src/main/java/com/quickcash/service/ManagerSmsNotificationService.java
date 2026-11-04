
package com.quickcash.service;

import com.quickcash.dto.ManagerSmsNotificationResponse;
import com.quickcash.entity.Loan;
import com.quickcash.entity.Payment;
import com.quickcash.entity.SmsNotification;
import com.quickcash.entity.SmsNotificationStatus;
import com.quickcash.repository.SmsNotificationRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerSmsNotificationService {

    private final SmsNotificationRepository repository;

    public ManagerSmsNotificationService(
            SmsNotificationRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<ManagerSmsNotificationResponse> getNotifications(
            SmsNotificationStatus status,
            int page,
            int size
    ) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt",
                        "smsId"
                )
        );

        Page<SmsNotification> results =
                status == null
                        ? repository.findAll(pageable)
                        : repository.findByStatus(status, pageable);

        return results.map(sms -> {

            Payment payment = sms.getPayment();
            Loan loan = payment.getLoan();

            return new ManagerSmsNotificationResponse(
                    sms.getSmsId(),
                    payment.getPaymentId(),
                    payment.getPaymentCode(),
                    loan.getLoanId(),
                    loan.getLoanCode(),
                    payment.getAmount(),
                    sms.getStatus(),
                    sms.getAttemptCount(),
                    sms.getCreatedAt(),
                    sms.getProcessingStartedAt(),
                    sms.getSentAt(),
                    sms.getLastError()
            );
        });
    }
}
