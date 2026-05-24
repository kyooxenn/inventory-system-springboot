package com.java.inventory.system.config;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import okhttp3.logging.HttpLoggingInterceptor;

@Configuration
public class MailjetConfig {

    @Bean
    public OkHttpClient mailjetHttpClient() {
        try {
            // Force TLS 1.2 (most stable with Mailjet)
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            };
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // Enable detailed SSL debugging (remove in production)
            System.setProperty("javax.net.debug", "ssl:handshake:verbose");
            
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> {
                    // Accept Mailjet's certificates even if hostname mismatch (debug only)
                    return hostname.equals("api.mailjet.com") || hostname.equals("api.mailjet.eu");
                })
                .retryOnConnectionFailure(true)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request request = chain.request()
                        .newBuilder()
                        .header("Connection", "close")  // Force new connection each time
                        .build();
                    return chain.proceed(request);
                })
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure Mailjet client", e);
        }
    }
}