package com.java.inventory.system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class HelloController {

    @GetMapping("/api/hello")
    public ResponseEntity<String> hello(Authentication authentication) {
        // This will only be reachable if a valid JWT is sent in Authorization header
        return ResponseEntity.ok("Hello " + authentication.getName());
    }
}
