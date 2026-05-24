package com.java.inventory.system.service;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.transactional.SendContact;
import com.mailjet.client.transactional.SendEmailsRequest;
import com.mailjet.client.transactional.TrackOpens;
import com.mailjet.client.transactional.TransactionalEmail;
import com.mailjet.client.transactional.response.SendEmailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    @Value("${MJ_APIKEY_PUBLIC}")
    private String apiKey;

    @Value("${MJ_APIKEY_PRIVATE}")
    private String apiSecret;

    private MailjetClient client;

    @PostConstruct
    public void init() {
        // Build an explicit, isolated connection layer
        ClientOptions options = ClientOptions.builder()
                .baseUrl("https://api.mailjet.com") // Forces standard safe web route
                .apiKey(apiKey)
                .apiSecretKey(apiSecret)
                .build();

        this.client = new MailjetClient(options);
    }

    public void sendOtpEmail(String toEmail, String otp) throws Exception {
        try {
            TransactionalEmail message = TransactionalEmail
                    .builder()
                    .to(new SendContact(toEmail, toEmail))
                    .from(new SendContact("norbertbobila12@gmail.com", "Norbert Jon Bobila"))
                    .htmlPart("<div style=\"font-family: Arial, sans-serif; text-align: center; padding: 20px;\">" +
                            "<h2 style=\"color: #333;\">Your One-Time Password</h2>" +
                            "<p style=\"font-size: 16px; color: #555;\">Use the code below to complete your verification:</p>" +
                            "<div style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; margin: 20px 0; color: #000;\">" +
                            otp +
                            "</div>" +
                            "<p style=\"font-size: 14px; color: #888;\">This code will expire in <b>5 minutes</b>.</p>" +
                            "</div>")
                    .subject("Your One-Time Password (OTP)")
                    .trackOpens(TrackOpens.ENABLED)
                    .build();

            SendEmailsRequest request = SendEmailsRequest
                    .builder()
                    .message(message)
                    .build();

            // Reuses the established client bean instance
            SendEmailsResponse responses = request.sendWith(this.client);
            log.info("Mailjet send response status: {}", responses);

        } catch (Exception ex) {
            log.error("Mailjet Communication Exception: {}", ex.getMessage());
            throw ex;
        }
    }
}
