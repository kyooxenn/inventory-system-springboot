package com.java.inventory.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    @Value("${BREVO_API_KEY}")
    private String apiKey;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3";

    public void sendOtpEmail(String toEmail, String otp) {
        // 1. Build the request body according to Brevo's API spec
        Map<String, Object> requestBody = Map.of(
                "sender", Map.of(
                        "email", "norbertbobila12@gmail.com",  // Your verified sender email
                        "name", "From N-Vent"
                ),
                "to", List.of(
                        Map.of(
                                "email", toEmail,
                                "name", toEmail.split("@")[0]  // Extract name from email
                        )
                ),
                "subject", "Your One-Time Password (OTP)",
                "htmlContent", buildOtpHtmlContent(otp)
        );


        // 2. Initialize RestClient with Brevo authentication
        RestClient restClient = RestClient.builder()
                .baseUrl(BREVO_API_URL)
                .defaultHeader("api-key", apiKey)  // Note: "api-key" not "Authorization"
                .defaultHeader("Content-Type", "application/json")
                .build();

        try {
            // 3. Send the POST request to Brevo's transactional email endpoint
            String response = restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("OTP email sent successfully via Brevo to: {}. Response: {}", toEmail, response);
        } catch (Exception e) {
            log.error("Failed to send OTP email via Brevo to: {}. Error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }



    private String buildOtpHtmlContent(String otp) {
        return "<div style=\"font-family: Arial, sans-serif; text-align: center; padding: 20px;\">" +
                "<h2 style=\"color: #333;\">Your One-Time Password</h2>" +
                "<p style=\"font-size: 16px; color: #555;\">Use the code below to complete your verification:</p>" +
                "<div style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; margin: 20px 0; color: #000;\">" +
                otp +
                "</div>" +
                "<p style=\"font-size: 14px; color: #888;\">This code will expire in <b>5 minutes</b>.</p>" +
                "</div>";
    }

}
