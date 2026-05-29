package com.java.inventory.system.controller;

import com.java.inventory.system.dto.AuthenticationRequest;
import com.java.inventory.system.dto.OtpVerificationRequest;
import com.java.inventory.system.service.AuthService;
import com.java.inventory.system.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthService authService;
  private final OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthenticationRequest request) {
        return authService.register(request);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtpEmail(@RequestBody OtpVerificationRequest request,
                                          HttpServletRequest servletRequest) {
        return otpService.sendOtp(request, servletRequest);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationRequest request) {
        return otpService.validateOtp(request);
    }
}
