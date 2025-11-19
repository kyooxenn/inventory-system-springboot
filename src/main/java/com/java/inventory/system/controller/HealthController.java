package com.java.inventory.system.controller;

import com.java.inventory.system.repository.ProductRepository;
import com.java.inventory.system.service.RedisViewerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final RedisViewerService redisViewerService;
    private final ProductRepository productRepository;

    @GetMapping("/api/health")
    public ResponseEntity<String> health() {
        try {
            Map<String, Object> serviceAllKeyValues = redisViewerService.getAllKeyValues();
            log.info("serviceAllKeyValues: [{}]", serviceAllKeyValues);

            List<?> products = productRepository.findAll();
            log.info("loaded all products: [{}]", products);

            String message = "✅ Inventory System API is alive! All systems operational 🚀";
            return ResponseEntity.ok(message);
        } catch (Exception ex) {
            String errorMessage = "❌ Service Unavailable: Inventory System is currently down. Please try again later.";
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorMessage);
        }
    }
}
