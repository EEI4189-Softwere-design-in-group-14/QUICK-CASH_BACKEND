package com.quickcash.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@ConditionalOnProperty(
        name = "quickcash.sms.provider",
        havingValue = "textbee"
)
public class TextBeeSmsGateway implements SmsGateway {

    private static final String API_URL =
            "https://api.textbee.dev/api/v1/gateway/send-sms";

    private final String apiKey;
    private final String deviceId;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TextBeeSmsGateway(
            @Value("${quickcash.sms.textbee.api-key:}")
            String apiKey,

            @Value("${quickcash.sms.textbee.device-id:}")
            String deviceId
    ) {

        this.apiKey = apiKey;
        this.deviceId = deviceId;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        this.objectMapper = new ObjectMapper();
    }

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

        try {

            Map<String, Object> payload =
                    new HashMap<>();

            payload.put(
                    "recipients",
                    List.of(recipient)
            );

            payload.put("message", message);

            // Select a specific registered device.
            if (deviceId != null
                    && !deviceId.isBlank()) {

                payload.put("deviceId", deviceId);
            }

            String requestBody =
                    objectMapper.writeValueAsString(
                            payload
                    );

            HttpRequest request =
                    HttpRequest.newBuilder()

                            .uri(URI.create(API_URL))

                            .timeout(
                                    Duration.ofSeconds(10)
                            )

                            .header(
                                    "x-api-key",
                                    apiKey
                            )

                            .header(
                                    "Content-Type",
                                    "application/json"
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

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            int status = response.statusCode();

            if (status < 200 || status >= 300) {

                throw new IllegalStateException(
                        "TextBee rejected SMS request"
                );
            }

            // Do not print API keys, OTPs,
            // phone numbers or SMS message bodies.
            // HTTP success means accepted, not delivered.

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "TextBee request interrupted"
            );

        } catch (Exception exception) {

            // Never automatically retry uncertain deliveries.
            throw new IllegalStateException(
                    "TextBee SMS submission failed"
            );
        }
    }

    private void validateConfiguration() {

        if (apiKey == null || apiKey.isBlank()) {

            throw new IllegalStateException(
                    "TextBee API Key is missing"
            );
        }
    }

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

            return "+94" + number.substring(1);
        }

        if (number.matches("^947[0-9]{8}$")) {

            return "+" + number;
        }

        if (number.matches("^\\+947[0-9]{8}$")) {

            return number;
        }

        throw new IllegalArgumentException(
                "Invalid Sri Lankan mobile number"
        );
    }
}