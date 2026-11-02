
package com.quickcash.service;

import com.quickcash.entity.SmsNotification;
import com.quickcash.entity.SmsNotificationStatus;
import com.quickcash.repository.SmsNotificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class SmsDispatchServiceTest {

    @Mock
    private SmsNotificationRepository smsRepository;

    @Mock
    private SmsGateway smsGateway;

    @InjectMocks
    private SmsDispatchService smsDispatchService;

    // Create a sample SMS for unit testing.
    private SmsNotification createTestSms() {

        SmsNotification sms = new SmsNotification();

        ReflectionTestUtils.setField(
                sms, "smsId", 101L
        );

        sms.setRecipientPhone("0771234567");
        sms.setMessageBody("QuickCash test SMS");
        sms.setStatus(SmsNotificationStatus.PENDING);

        return sms;
    }

    // TEST 1: Successful SMS processing
    @Test
    void successfulSmsIsMarkedSent() {

        SmsNotification sms = createTestSms();

        when(smsRepository
                .findTop10ByStatusOrderByCreatedAtAsc(
                        SmsNotificationStatus.PENDING
                ))
                .thenReturn(List.of(sms));


        when(smsRepository.claimSms(
                eq(101L),
                eq(SmsNotificationStatus.PENDING),
                eq(SmsNotificationStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(1);

        when(smsRepository.findById(101L))
                .thenReturn(Optional.of(sms));

        when(smsRepository.markSent(
                eq(101L),
                eq(SmsNotificationStatus.PROCESSING),
                eq(SmsNotificationStatus.SENT),
                any(LocalDateTime.class)
        )).thenReturn(1);

        smsDispatchService.processPendingSms();

        verify(smsGateway, times(1))
                .sendSms(
                        "0771234567",
                        "QuickCash test SMS"
                );

        verify(smsRepository, times(1))
                .markSent(
                        eq(101L),
                        eq(SmsNotificationStatus.PROCESSING),
                        eq(SmsNotificationStatus.SENT),
                        any(LocalDateTime.class)
                );
    }

    // TEST 2: Another worker already claimed the SMS
    @Test
    void alreadyClaimedSmsIsNotSentAgain() {

        SmsNotification sms = createTestSms();

        when(smsRepository
                .findTop10ByStatusOrderByCreatedAtAsc(
                        SmsNotificationStatus.PENDING
                ))
                .thenReturn(List.of(sms));



        when(smsRepository.claimSms(
                eq(101L),
                eq(SmsNotificationStatus.PENDING),
                eq(SmsNotificationStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(0);


        smsDispatchService.processPendingSms();

        verifyNoInteractions(smsGateway);

        verify(smsRepository, never())
                .markSent(
                        anyLong(),
                        any(),
                        any(),
                        any(LocalDateTime.class)
                );
    }

    // TEST 3: Gateway failure must be recorded
    @Test
    void gatewayFailureIsMarkedFailed() {

        SmsNotification sms = createTestSms();

        when(smsRepository
                .findTop10ByStatusOrderByCreatedAtAsc(
                        SmsNotificationStatus.PENDING
                ))
                .thenReturn(List.of(sms));




        when(smsRepository.claimSms(
                eq(101L),
                eq(SmsNotificationStatus.PENDING),
                eq(SmsNotificationStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(1);



        when(smsRepository.findById(101L))
                .thenReturn(Optional.of(sms));

        doThrow(new RuntimeException(
                "Mock gateway unavailable"
        )).when(smsGateway).sendSms(
                "0771234567",
                "QuickCash test SMS"
        );

        smsDispatchService.processPendingSms();

        verify(smsRepository, times(1))
                .markFailed(
                        eq(101L),
                        eq(SmsNotificationStatus.PROCESSING),
                        eq(SmsNotificationStatus.FAILED),
                        anyString()
                );

        verify(smsRepository, never())
                .markSent(
                        anyLong(),
                        any(),
                        any(),
                        any(LocalDateTime.class)
                );
    }


    @Test
    void stuckSmsIsFlaggedWithoutResending() {

        when(smsRepository.markStuckProcessingForReview(
                eq(SmsNotificationStatus.PROCESSING),
                eq(SmsNotificationStatus.FAILED),
                any(LocalDateTime.class)
        )).thenReturn(2);

        smsDispatchService.recoverStuckSms();

        verify(smsRepository, times(1))
                .markStuckProcessingForReview(
                        eq(SmsNotificationStatus.PROCESSING),
                        eq(SmsNotificationStatus.FAILED),
                        any(LocalDateTime.class)
                );

        // Never send SMS again automatically.
        verifyNoInteractions(smsGateway);
    }

}
