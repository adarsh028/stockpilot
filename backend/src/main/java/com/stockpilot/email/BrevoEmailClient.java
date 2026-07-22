package com.stockpilot.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over the Brevo transactional email API v3. The dev fallback (logging
 * instead of sending when the key is blank or the call fails) lives in the callers
 * ({@link EmailOtpSender}, {@link BrevoEmailService}); this class only performs the HTTP call.
 */
@Slf4j
@Component
public class BrevoEmailClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailClient(
            @Value("${brevo.base-url}") String baseUrl,
            @Value("${brevo.api-key:}") String apiKey,
            @Value("${brevo.sender-email}") String senderEmail,
            @Value("${brevo.sender-name}") String senderName) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void sendTransactionalEmail(String toEmail, String toName, String subject, String htmlContent) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", senderEmail, "name", senderName),
                "to", List.of(Map.of("email", toEmail, "name", toName == null ? toEmail : toName)),
                "subject", subject,
                "htmlContent", htmlContent
        );
        restClient.post()
                .uri("/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
