package com.java.inventory.system.service;

import com.java.inventory.system.repository.ProductRepository;
import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.management.ManagementFactory;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeepAliveService {

    private final RedisViewerService redisViewerService;
    private final ProductRepository productRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.keepalive.health-url}")
    private String healthCheckUrl;

    private static final double CPU_THRESHOLD = 0.90; // 90%
    private static final int MAX_HIGH_CPU_CHECKS = 5; // e.g. 5 consecutive minutes
    private int consecutiveHighCpuCount = 0;

    // ✅ Adaptive scheduler: checks every minute and pings based on time of day
    @Scheduled(fixedRate = 60000) // every 1 minute
    public void adaptivePingLoginApi() {
        try {
            LocalTime now = LocalTime.now(ZoneId.of("Asia/Singapore"));
            int hour = now.getHour();

            // ✅ 6 AM – 12 AM → ping every 5 min
            // ✅ 12 AM – 6 AM → ping every 10 min
            boolean isDayTime = (hour >= 6 && hour < 24);
            int pingIntervalMinutes = isDayTime ? 5 : 10;

            int currentMinute = now.getMinute();
            if (currentMinute % pingIntervalMinutes != 0) {
                return; // Skip if not time to ping yet
            }

            // ✅ Monitor CPU usage
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemCpuLoad(); // 0.0 to 1.0

            if (cpuLoad >= 0 && cpuLoad < CPU_THRESHOLD) {
                consecutiveHighCpuCount = 0; // reset if CPU is normal
                log.info("⏱️ Keep-alive check at {} (interval: {} min, CPU: {}%)",
                        now, pingIntervalMinutes, (int) (cpuLoad * 100));

                Map<String, Object> serviceAllKeyValues = redisViewerService.getAllKeyValues();
                log.info("serviceAllKeyValues: [{}]", serviceAllKeyValues);

                List<?> products = productRepository.findAll();
                log.info("loaded all products: [{}]", products);

                ResponseEntity<String> response =
                        restTemplate.getForEntity(healthCheckUrl, String.class);

                log.info("Keep-alive response: {}", response.getBody());

            } else {
                consecutiveHighCpuCount++;
                log.warn("⚠️ High CPU detected ({}%). Count: {}/{}",
                        (int) (cpuLoad * 100), consecutiveHighCpuCount, MAX_HIGH_CPU_CHECKS);

                // If high CPU persists for too long, restart gracefully
                if (consecutiveHighCpuCount >= MAX_HIGH_CPU_CHECKS) {
                    log.error("💥 CPU usage stayed above {}% for {} checks. Restarting app gracefully...",
                            (int) (CPU_THRESHOLD * 100), MAX_HIGH_CPU_CHECKS);
                    System.exit(1); // Render will auto-restart the service
                }
            }

        } catch (Exception e) {
            log.warn("⚠️ Failed to ping health check API: {}", e.getMessage());
        }
    }
}
