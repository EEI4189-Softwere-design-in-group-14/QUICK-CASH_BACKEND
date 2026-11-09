
package com.quickcash.service;

import com.quickcash.entity.*;
import com.quickcash.repository.SmsNotificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerSmsNotificationServiceTest {

    @Mock
    private SmsNotificationRepository repository;

    @InjectMocks
    private ManagerSmsNotificationService service;

    private SmsNotification createSms(
            SmsNotificationStatus status) {

        Loan loan = new Loan();
        loan.setLoanId(5L);
        loan.setLoanCode("LN-TEST-5");

        Payment payment = new Payment();
        payment.setPaymentId(21L);
        payment.setPaymentCode("PAY-TEST-21");
        payment.setAmount(new BigDecimal("110.00"));
        payment.setLoan(loan);

        SmsNotification sms = new SmsNotification();
        sms.setPayment(payment);
        sms.setStatus(status);
        sms.setAttemptCount(1);
        sms.setRecipientPhone("0771234567");
        sms.setMessageBody("Private test message");

        return sms;
    }

    @Test
    void managerCanViewSmsList() {

        var sms = createSms(SmsNotificationStatus.SENT);

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sms)));

        var result = service.getNotifications(null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(21L, result.getContent().get(0).paymentId());
        assertEquals(5L, result.getContent().get(0).loanId());
        assertEquals(SmsNotificationStatus.SENT,
                result.getContent().get(0).status());
    }

    @Test
    void managerCanFilterFailedSms() {

        var sms = createSms(SmsNotificationStatus.FAILED);
        sms.setLastError("Manual review required");

        when(repository.findByStatus(
                eq(SmsNotificationStatus.FAILED),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(sms)));

        var result = service.getNotifications(
                SmsNotificationStatus.FAILED, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Manual review required",
                result.getContent().get(0).lastError());

        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void invalidPaginationReturnsBadRequest() {

        for (int[] values : new int[][] {
                {-1, 20},
                {0, 0},
                {0, 101}
        }) {
            var error = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getNotifications(
                            null, values[0], values[1])
            );

            assertEquals(
                    HttpStatus.BAD_REQUEST,
                    error.getStatusCode()
            );
        }

        verifyNoInteractions(repository);
    }

    @Test
    void smsDashboardDoesNotExposePrivateDetails() {

        var fields = java.util.Arrays.stream(
                        com.quickcash.dto
                                .ManagerSmsNotificationResponse.class
                                .getRecordComponents()
                ).map(java.lang.reflect.RecordComponent::getName)
                .toList();

        assertFalse(fields.contains("recipientPhone"));
        assertFalse(fields.contains("messageBody"));
    }
}
