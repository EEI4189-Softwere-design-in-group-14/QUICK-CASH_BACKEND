package com.quickcash.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextBeeSmsGatewayTest {

    @Test
    void localPhoneNumberIsNormalized() {

        assertEquals(
                "+94771234567",
                TextBeeSmsGateway.normalizePhoneNumber(
                        "0771234567"
                )
        );
    }

    @Test
    void internationalNumberIsAccepted() {

        assertEquals(
                "+94771234567",
                TextBeeSmsGateway.normalizePhoneNumber(
                        "+94771234567"
                )
        );
    }

    @Test
    void countryCodeIsNormalized() {

        assertEquals(
                "+94771234567",
                TextBeeSmsGateway.normalizePhoneNumber(
                        "94771234567"
                )
        );
    }

    @Test
    void formattedNumberIsNormalized() {

        assertEquals(
                "+94771234567",
                TextBeeSmsGateway.normalizePhoneNumber(
                        "077-123-4567"
                )
        );
    }

    @Test
    void invalidPhoneNumberIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> TextBeeSmsGateway.normalizePhoneNumber(
                        "12345"
                )
        );
    }

    @Test
    void missingPhoneNumberIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> TextBeeSmsGateway.normalizePhoneNumber(
                        null
                )
        );
    }

    @Test
    void missingApiKeyIsRejected() {

        TextBeeSmsGateway gateway =
                new TextBeeSmsGateway("", "");

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

        TextBeeSmsGateway gateway =
                new TextBeeSmsGateway(
                        "test-key",
                        ""
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