package com.java.inventory.system.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
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

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", "norbertbobila12@gmail.com")
                                            .put("Name", "Norbert Jon Bobila"))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", toEmail)
                                                    .put("Name",toEmail)))
                                    .put(Emailv31.Message.SUBJECT, "Your One-Time Password (OTP)")
                                    .put(Emailv31.Message.HTMLPART, "<div style=\"font-family: Arial, sans-serif; text-align: center; padding: 20px;\">" +
                                            "<h2 style=\"color: #333;\">Your One-Time Password</h2>" +
                                            "<p style=\"font-size: 16px; color: #555;\">Use the code below to complete your verification:</p>" +
                                            "<div style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; margin: 20px 0; color: #000;\">" +
                                            otp +
                                            "</div>" +
                                            "<p style=\"font-size: 14px; color: #888;\">This code will expire in <b>5 minutes</b>.</p>" +
                                            "<hr style=\"margin: 30px 0;\">" +
                                            "<p style=\"font-size: 12px; color: #aaa;\">If you didn’t request this, you can safely ignore this email.</p>" +
                                            "</div>")));


            MailjetResponse response = client.post(request);

            log.info("status for sendOtpEmail: {}", response.getStatus());

            if (response.getStatus() == 401 || response.getStatus() == 403) {

                // Parse the JSON string
                JsonObject jsonObject = JsonParser.parseString(response.getRawResponseContent()).getAsJsonObject();
                // Get the "errors" array
                JsonArray errorsArray = jsonObject.getAsJsonArray("errors");

                // Check if the array has elements and extract the first error's message
                if (!errorsArray.isEmpty()) {
                    JsonObject firstError = errorsArray.get(0).getAsJsonObject();
                    String message = firstError.get("message").getAsString();
                    log.error("error: {}", message);
                } else {
                    log.info("No errors found for sendOtpEmail");
                }
            } else {
                log.info("📧 Email sent to {} | Status: {}", toEmail, response.getStatus());
            }
        } catch (MailjetException ex) {
            throw ex;
        }
    }
}
