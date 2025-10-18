package com.java.inventory.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.Map;

@Slf4j
@Service
public class KeepAliveService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.keepalive.login-url}")
    private String loginUrl;

    // ✅ Adaptive scheduler: checks every minute and pings based on time of day
    @Scheduled(fixedRate = 60000) // run every 1 minute
    public void adaptivePingLoginApi() {
        try {
            LocalTime now = LocalTime.now();
            int hour = now.getHour();

            // ✅ 6 AM – 12 AM → ping every 5 min
            // ✅ 12 AM – 6 AM → ping every 30 min
            boolean isDayTime = (hour >= 6 && hour < 24);
            int pingIntervalMinutes = isDayTime ? 5 : 30;

            int currentMinute = now.getMinute();
            if (currentMinute % pingIntervalMinutes != 0) {
                return; // Skip ping to save CPU during non-interval minutes
            }

            log.info("⏱️ Keep-alive check at {} (interval: {} min)", now, pingIntervalMinutes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Customize your credentials here
            Map<String, String> body = Map.of("username", "root", "password", "root");
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, request, String.class);

            log.info("✅ Keep-alive response: {}", response.getStatusCode());

        } catch (Exception e) {
            log.warn("⚠️ Failed to ping login API: {}", e.getMessage());
        }
    }
}
