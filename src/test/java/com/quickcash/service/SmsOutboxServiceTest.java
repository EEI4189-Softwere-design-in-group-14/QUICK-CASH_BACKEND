
package com.quickcash.service;

import com.quickcash.entity.*;
import com.quickcash.repository.SmsNotificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsOutboxServiceTest {

    @Mock
    private SmsNotificationRepository smsRepository;

    @Test
    void missingPhoneSkipsSmsWithoutException() {

        User borrowerUser = new User();
        borrowerUser.setPhone("");

        Borrower borrower = new Borrower();
        borrower.setUser(borrowerUser);

        Loan loan = new Loan();
        loan.setBorrower(borrower);

        Payment payment = new Payment();
        payment.setPaymentId(999L);
        payment.setLoan(loan);

        when(smsRepository.findByPayment_PaymentId(999L))
                .thenReturn(Optional.empty());

        SmsOutboxService service =
                new SmsOutboxService(smsRepository);

        // Missing phone must not throw an exception.
        assertDoesNotThrow(
                () -> service.queuePaymentSms(payment)
        );

        // No SMS should be saved without a phone number.
        verify(smsRepository, never())
                .save(any(SmsNotification.class));
    }
}
