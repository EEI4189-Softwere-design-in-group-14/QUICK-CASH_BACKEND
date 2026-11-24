package com.quickcash.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotifySmsGatewayTest {

    @Test
    void localPhoneNumberIsNormalized() {

        assertEquals(
                "94771234567",
                NotifySmsGateway.normalizePhoneNumber(
                        "0771234567"
                )
        );
    }

    @Test
    void internationalPhoneNumberIsNormalized() {

        assertEquals(
                "94771234567",
                NotifySmsGateway.normalizePhoneNumber(
                        "+94771234567"
                )
        );
    }

    @Test
    void alreadyNormalizedNumberIsAccepted() {

        assertEquals(
                "94771234567",
                NotifySmsGateway.normalizePhoneNumber(
                        "94771234567"
                )
        );
    }

    @Test
    void formattedPhoneNumberIsNormalized() {

        assertEquals(
                "94771234567",
                NotifySmsGateway.normalizePhoneNumber(
                        "077-123-4567"
                )
        );
    }

    @Test
    void invalidPhoneNumberIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> NotifySmsGateway.normalizePhoneNumber(
                        "12345"
                )
        );
    }

    @Test
    void missingPhoneNumberIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> NotifySmsGateway.normalizePhoneNumber(
                        null
                )
        );
    }

    @Test
    void missingCredentialsAreRejected() {

        NotifySmsGateway gateway =
                new NotifySmsGateway(
                        "",
                        "",
                        ""
                );

        assertThrows(
                IllegalStateException.class,
                () -> gateway.sendSms(
                        "0771234567",
                        "Test message"
                )
        );
    }

    @Test
    void demoSenderIsRejected() {

        NotifySmsGateway gateway =
                new NotifySmsGateway(
                        "test-user",
                        "test-key",
                        "NotifyDEMO"
                );

        assertThrows(
                IllegalStateException.class,
                () -> gateway.sendSms(
                        "0771234567",
                        "Test message"
                )
        );
    }

    @Test
    void emptyMessageIsRejected() {

        NotifySmsGateway gateway =
                new NotifySmsGateway(
                        "test-user",
                        "test-key",
                        "TestSender"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> gateway.sendSms(
                        "0771234567",
                        ""
                )
        );
    }
}