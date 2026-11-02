
package com.quickcash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "quickcash.sms.provider",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockSmsGateway implements SmsGateway {

    private static final Logger log =
            LoggerFactory.getLogger(MockSmsGateway.class);

    @Override
    public void sendSms(String phoneNumber, String message) {

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "SMS recipient phone number is required"
            );
        }

        String maskedPhone =
                phoneNumber.length() > 4
                        ? "****" + phoneNumber.substring(
                        phoneNumber.length() - 4
                )
                        : "****";

        log.info(
                "MOCK SMS processed for recipient {}",
                maskedPhone
        );
    }
}

