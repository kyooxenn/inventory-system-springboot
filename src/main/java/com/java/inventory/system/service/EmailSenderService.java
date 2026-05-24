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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    @Value("${MJ_APIKEY_PUBLIC}")
    private String apiKey;

    @Value("${MJ_APIKEY_PRIVATE}")
    private String apiSecret;

    public void sendOtpEmail(String toEmail, String otp) throws MailjetException {
        try {

            ClientOptions options = ClientOptions.builder()
                    .apiKey(apiKey)
                    .apiSecretKey(apiSecret)
                    .build();

            MailjetClient client = new MailjetClient(options);

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
                            "<hr style=\"margin: 30px 0;\">" +
                            "<p style=\"font-size: 12px; color: #aaa;\">If you didn’t request this, you can safely ignore this email.</p>" +
                            "</div>")
                    .subject("Your One-Time Password (OTP)")
                    .trackOpens(TrackOpens.ENABLED)
                    .header("test-header-key", "test-value")
                    .customID("custom-id-value")
                    .build();

            SendEmailsRequest request = SendEmailsRequest
                    .builder()
                    .message(message)
                    .build();

            SendEmailsResponse responses = request.sendWith(client);
            log.info("status for responses: {}", responses);


        } catch (MailjetException ex) {
            log.error("MailjetException error message: [{}]", ex.getMessage());
            throw ex;
        }
    }
}
