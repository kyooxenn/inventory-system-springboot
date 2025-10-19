package com.java.inventory.system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity<String> health() {
        try {
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
