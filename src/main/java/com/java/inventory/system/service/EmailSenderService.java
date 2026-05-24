package com.java.inventory.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Base64;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    @Value("${MJ_APIKEY_PUBLIC}")
    private String apiKey;

    @Value("${MJ_APIKEY_PRIVATE}")
    private String apiSecret;

    public void sendOtpEmail(String toEmail, String otp) {
        // 1. Initialize Spring's native HTTP Client with standard credentials
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((apiKey + ":" + apiSecret).getBytes());
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.mailjet.com/v3.1")
                .defaultHeader("Authorization", authHeader)
                .build();

        // 2. Build the exact JSON body Mailjet expects (v3.1 Payload)
        Map<String, Object> body = Map.of(
                "Messages", List.of(
                        Map.of(
                                "From", Map.of("Email", "norbertbobila12@gmail.com", "Name", "Norbert Jon Bobila"),
                                "To", List.of(Map.of("Email", toEmail, "Name", toEmail)),
                                "Subject", "Your One-Time Password (OTP)",
                                "HTMLPart", "<div style=\"font-family: Arial, sans-serif; text-align: center; padding: 20px;\">" +
                                        "<h2 style=\"color: #333;\">Your One-Time Password</h2>" +
                                        "<p style=\"font-size: 16px; color: #555;\">Use the code below to complete your verification:</p>" +
                                        "<div style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; margin: 20px 0; color: #000;\">" +
                                        otp +
                                        "</div>" +
                                        "<p style=\"font-size: 14px; color: #888;\">This code will expire in <b>5 minutes</b>.</p>" +
                                        "</div>"
                        )
                )
        );

        try {
            // 3. Fire the request securely over HTTPS Port 443
            String response = restClient.post()
                    .uri("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("Email sent successfully via Spring RestClient: {}", response);
        } catch (Exception e) {
            log.error("Failed to send email via standard HTTPS: {}", e.getMessage());
            throw e;
        }
    }
}
