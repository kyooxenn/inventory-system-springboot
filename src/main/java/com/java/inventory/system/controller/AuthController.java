package com.java.inventory.system.controller;

import com.java.inventory.system.dto.AuthRequest;
import com.java.inventory.system.dto.OtpVerificationRequest;
import com.java.inventory.system.service.AuthService;
import com.java.inventory.system.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtpEmail(@RequestBody OtpVerificationRequest request) throws IOException {
        return otpService.sendOtpEmail(request);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtpEmail(@RequestBody OtpVerificationRequest request) {
        return otpService.verifyOtpEmail(request);
    }
}
