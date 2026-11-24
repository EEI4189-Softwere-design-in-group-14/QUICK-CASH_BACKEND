package com.quickcash.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OtpDeliveryServiceTest {

    private SmsGateway smsGateway;

    private OtpDeliveryService otpDeliveryService;

    @BeforeEach
    void setUp() {

        smsGateway = mock(SmsGateway.class);

        otpDeliveryService = new OtpDeliveryService(
                smsGateway,
                "test-provider"
        );
    }

    // Test 1: Valid OTP is submitted to the gateway.
    @Test
    void validOtpIsSubmittedToGateway() {

        otpDeliveryService.sendOtp(
                "0770000000",
                "123456"
        );

        verify(smsGateway, times(1)).sendSms(
                eq("0770000000"),
                contains("123456")
        );
    }

    // Test 2: Missing phone number is rejected.
    @Test
    void missingPhoneNumberIsRejected() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> otpDeliveryService.sendOtp(
                                null,
                                "123456"
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(smsGateway);
    }

    // Test 3: Blank phone number is rejected.
    @Test
    void blankPhoneNumberIsRejected() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> otpDeliveryService.sendOtp(
                                "",
                                "123456"
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(smsGateway);
    }

    // Test 4: Invalid OTP format is rejected.
    @Test
    void invalidOtpFormatIsRejected() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> otpDeliveryService.sendOtp(
                                "0770000000",
                                "123"
                        )
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatusCode()
        );

        verifyNoInteractions(smsGateway);
    }

    // Test 5: Mock provider cannot report real delivery.
    @Test
    void mockProviderIsRejected() {

        OtpDeliveryService mockDeliveryService =
                new OtpDeliveryService(
                        smsGateway,
                        "mock"
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> mockDeliveryService.sendOtp(
                                "0770000000",
                                "123456"
                        )
                );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );

        verifyNoInteractions(smsGateway);
    }

    // Test 6: SMS gateway failure is handled safely.
    @Test
    void gatewayFailureReturnsServiceUnavailable() {

        doThrow(new RuntimeException("Gateway failure"))
                .when(smsGateway)
                .sendSms(anyString(), anyString());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> otpDeliveryService.sendOtp(
                                "0770000000",
                                "123456"
                        )
                );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );

        assertEquals(
                "OTP delivery failed",
                exception.getReason()
        );
    }

    // Test 7: OTP must contain exactly six digits.
    @Test
    void alphabeticOtpIsRejected() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> otpDeliveryService.sendOtp(
                                "0770000000",
                                "ABCDEF"
                        )
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatusCode()
        );

        verifyNoInteractions(smsGateway);
    }

    // Test 8: OTP with leading zeros is accepted.
    @Test
    void otpWithLeadingZerosIsAccepted() {

        otpDeliveryService.sendOtp(
                "0770000000",
                "001234"
        );

        verify(smsGateway).sendSms(
                eq("0770000000"),
                contains("001234")
        );
    }
}