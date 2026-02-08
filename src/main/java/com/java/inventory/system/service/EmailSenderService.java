package com.java.inventory.system.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    @Value("${SENDGRID_EMAIL}")
    private String sendGridEmail;

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    public void sendOtpEmail(String toEmail, String otp) throws IOException {
        Email from = new Email(sendGridEmail);
        String subject = "Your One-Time Password (OTP)";
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", "Your OTP code is: " + otp + "\n\nIt will expire in 5 minutes.");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);// sg.setDataResidency("eu");
        // uncomment the above line if you are sending mail using a regional EU subuser
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            log.info("status code for sendOtpEmail: {}", response.getStatusCode());

            if (response.getStatusCode() == 401 || response.getStatusCode() == 403) {

                // Parse the JSON string
                JsonObject jsonObject = JsonParser.parseString(response.getBody()).getAsJsonObject();
                // Get the "errors" array
                JsonArray errorsArray = jsonObject.getAsJsonArray("errors");

                // Check if the array has elements and extract the first error's message
                if (!errorsArray.isEmpty()) {
                    JsonObject firstError = errorsArray.get(0).getAsJsonObject();
                    String message = firstError.get("message").getAsString();
                    log.error("error: {}", message);
                    throw new BadRequestException(message);
                } else {
                    log.info("No errors found for sendOtpEmail");
                }
            } else {
                log.info("📧 Email sent to {} | Status: {}", toEmail, response.getStatusCode());
            }
        } catch (IOException ex) {
            throw ex;
        }
    }
}
