package com.java.inventory.system.service;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.errors.MailjetClientCommunicationException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.transactional.SendContact;
import com.mailjet.client.transactional.SendEmailsRequest;
import com.mailjet.client.transactional.TrackOpens;
import com.mailjet.client.transactional.TransactionalEmail;
import com.mailjet.client.transactional.response.SendEmailsResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
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

    private MailjetClient client;

    @PostConstruct
    public void init() {
        try {
            // 1. Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };

            // 2. Install the open trust manager into an SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 3. Build a custom OkHttpClient using the relaxed SSL context
            OkHttpClient relaxedClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true) // Bypass hostname matching
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // 4. Force Mailjet options to use standard HTTPS endpoint and our open client
            ClientOptions options = ClientOptions.builder()
                    .baseUrl("https://mailjet.com")
                    .apiKey(apiKey)
                    .apiSecretKey(apiSecret)
                    .okHttpClient(relaxedClient)
                    .build();

            this.client = new MailjetClient(options);
            log.info("MailjetClient initialized successfully with relaxed SSL options.");

        } catch (Exception e) {
            log.error("Failed to configure relaxed Mailjet SSL context. Falling back to default client configuration.", e);
            // Fallback initialization if something fails
            ClientOptions options = ClientOptions.builder()
                    .apiKey(apiKey)
                    .apiSecretKey(apiSecret)
                    .build();
            this.client = new MailjetClient(options);
        }
    }

    public void sendOtpEmail(String toEmail, String otp) throws MailjetClientCommunicationException, MailjetException {
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
            SendEmailsResponse response = request.sendWith(this.client);
            log.info("Mailjet send response status: {}", response);

        } catch (MailjetClientCommunicationException e) {
            log.error("MailjetClientCommunicationException: {}", e.getMessage());
            throw e;
        } catch (MailjetException ex) {
            log.error("MailjetException: {}", ex.getMessage());
            throw ex;
        }
    }
}
