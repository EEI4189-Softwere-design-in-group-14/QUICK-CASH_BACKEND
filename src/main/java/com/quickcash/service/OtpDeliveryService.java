package com.quickcash.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OtpDeliveryService {

    private final SmsGateway smsGateway;
    private final String smsProvider;

    public OtpDeliveryService(
            SmsGateway smsGateway,
            @Value("${quickcash.sms.provider:mock}")
            String smsProvider
    ) {
        this.smsGateway = smsGateway;
        this.smsProvider = smsProvider;
    }

    public void sendOtp(
            String phoneNumber,
            String otp
    ) {

        if (phoneNumber == null || phoneNumber.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Borrower phone number is missing"
            );
        }

        // Validate generated OTP.
        if (otp == null || !otp.matches("[0-9]{6}")) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "OTP generation failed"
            );
        }

        // Do not treat mock SMS processing as real delivery.
        if ("mock".equalsIgnoreCase(smsProvider)
                || smsGateway instanceof MockSmsGateway) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Real OTP delivery is not configured"
            );
        }

        String message =
                "Your QuickCash loan disbursement OTP is "
                        + otp
                        + ". Valid for 5 minutes. "
                        + "Share only with the authorized "
                        + "officer during disbursement.";

        try {

            smsGateway.sendSms(
                    phoneNumber,
                    message
            );

        } catch (Exception exception) {

            // Never expose OTP values or SMS message contents.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OTP delivery failed"
            );
        }
    }
}