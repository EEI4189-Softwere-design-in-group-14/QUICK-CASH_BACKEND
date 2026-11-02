
package com.quickcash.service;

import com.quickcash.entity.Payment;
import com.quickcash.entity.SmsNotification;
import com.quickcash.entity.SmsNotificationStatus;
import com.quickcash.repository.SmsNotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
public class SmsOutboxService {

    private final SmsNotificationRepository smsRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public SmsOutboxService(
            SmsNotificationRepository smsRepository
    ) {
        this.smsRepository = smsRepository;
    }

    // Must run inside the payment database transaction.
    @Transactional(propagation = Propagation.MANDATORY)
    public void queuePaymentSms(Payment payment) {

        if (payment == null || payment.getPaymentId() == null) {
            throw new IllegalArgumentException(
                    "Saved payment is required"
            );
        }

        // One SMS outbox entry per payment.
        if (smsRepository
                .findByPayment_PaymentId(payment.getPaymentId())
                .isPresent()) {
            return;
        }

        // SMS goes to the BORROWER ONLY.
        String borrowerPhone = payment.getLoan()
                .getBorrower()
                .getUser()
                .getPhone();


        if (borrowerPhone == null || borrowerPhone.isBlank()) {
            // Skip SMS when borrower has no phone number.
            // A missing phone number must not cancel a payment.
            return;
        }

        // Build SMS using the saved payment details.
        String message =
                "QuickCash Payment Received\n" +
                        "Loan: " + payment.getLoan().getLoanCode() + "\n" +
                        "Paid: LKR " + payment.getAmount() + "\n" +
                        "Date: " + payment.getPaymentDate()
                        .format(DATE_FORMAT) + "\n" +
                        "Collector: " + payment.getCollector()
                        .getFullName() + "\n" +
                        "Ref: " + payment.getPaymentCode() + "\n" +
                        "Balance: LKR " + payment.getBalanceAfter() + "\n" +
                        "Thank you.";

        SmsNotification sms = new SmsNotification();

        sms.setPayment(payment);
        sms.setRecipientPhone(borrowerPhone);
        sms.setMessageBody(message);
        sms.setStatus(SmsNotificationStatus.PENDING);
        sms.setAttemptCount(0);

        // This only saves the message to the database.
        // No SMS gateway request is made here.
        smsRepository.save(sms);
    }
}
