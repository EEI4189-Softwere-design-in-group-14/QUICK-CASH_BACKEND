package com.quickcash.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@ConditionalOnProperty(
        name = "quickcash.sms.provider",
        havingValue = "notify"
)
public class NotifySmsGateway implements SmsGateway {

    private static final String API_URL =
            "https://app.notify.lk/api/v1/send";

    private final String userId;
    private final String apiKey;
    private final String senderId;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NotifySmsGateway(
            @Value("${quickcash.sms.notify.user-id:}")
            String userId,

            @Value("${quickcash.sms.notify.api-key:}")
            String apiKey,

            @Value("${quickcash.sms.notify.sender-id:}")
            String senderId
    ) {

        this.userId = userId;
        this.apiKey = apiKey;
        this.senderId = senderId;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(
                        HttpClient.Redirect.NEVER
                )
                .build();

        this.objectMapper = new ObjectMapper();
    }

    // Send an SMS through Notify.lk.
    @Override
    public void sendSms(
            String phoneNumber,
            String message
    ) {

        validateConfiguration();

        String recipient =
                normalizePhoneNumber(phoneNumber);

        if (message == null || message.isBlank()) {

            throw new IllegalArgumentException(
                    "SMS message is required"
            );
        }

        if (message.length() > 621) {

            throw new IllegalArgumentException(
                    "SMS message is too long"
            );
        }

        // Never use the demo sender for production messages.
        if ("NotifyDEMO".equalsIgnoreCase(senderId)) {

            throw new IllegalStateException(
                    "An approved Sender ID is required"
            );
        }

        // Build form-encoded request body.
        String requestBody =
                "user_id=" + encode(userId)
                        + "&api_key=" + encode(apiKey)
                        + "&sender_id=" + encode(senderId)
                        + "&to=" + encode(recipient)
                        + "&message=" + encode(message);

        // Use POST so credentials are not in the URL.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))

                .timeout(Duration.ofSeconds(10))

                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                )

                .header(
                        "Accept",
                        "application/json"
                )

                .POST(
                        HttpRequest.BodyPublishers
                                .ofString(requestBody)
                )

                .build();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            // Check HTTP response status.
            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "SMS gateway rejected the request"
                );
            }

            // HTTP 200 alone does not guarantee success.
            JsonNode result =
                    objectMapper.readTree(
                            response.body()
                    );

            if (!"success".equals(
                    result.path("status").asText())) {

                throw new IllegalStateException(
                        "SMS gateway did not accept the message"
                );
            }

            // Do not log the OTP, API key,
            // request body or recipient phone number.

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "SMS request was interrupted"
            );

        } catch (Exception exception) {

            // Avoid exposing provider responses or secrets.
            // Do not automatically retry uncertain deliveries.
            throw new IllegalStateException(
                    "Notify.lk SMS submission failed"
            );
        }
    }

    // Validate SMS configuration.
    private void validateConfiguration() {

        if (userId == null || userId.isBlank()
                || apiKey == null || apiKey.isBlank()
                || senderId == null || senderId.isBlank()) {

            throw new IllegalStateException(
                    "Notify.lk configuration is incomplete"
            );
        }
    }

    // Convert Sri Lankan mobile numbers to international format.
    static String normalizePhoneNumber(
            String phoneNumber
    ) {

        if (phoneNumber == null
                || phoneNumber.isBlank()) {

            throw new IllegalArgumentException(
                    "Recipient phone number is required"
            );
        }

        String number = phoneNumber.trim()
                .replaceAll("[\\s()-]", "");

        if (number.matches("^07[0-9]{8}$")) {

            return "94" + number.substring(1);
        }

        if (number.matches("^\\+947[0-9]{8}$")) {

            return number.substring(1);
        }

        if (number.matches("^947[0-9]{8}$")) {

            return number;
        }

        throw new IllegalArgumentException(
                "Invalid Sri Lankan mobile number"
        );
    }

    // Encode values safely for the POST request.
    private static String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}