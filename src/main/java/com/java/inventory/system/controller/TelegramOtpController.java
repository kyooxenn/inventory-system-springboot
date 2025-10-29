package com.java.inventory.system.controller;

import com.java.inventory.system.service.CustomUserDetailsService;
import com.java.inventory.system.service.OtpService;
import com.java.inventory.system.util.OtpGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/telegram")
@Slf4j
@CrossOrigin
public class TelegramOtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CustomUserDetailsService userService; // For getting chatId

    // Helper method to validate tempToken and get userId
    private String validateTempTokenAndGetUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid tempToken");
        }
        String tempToken = authHeader.substring(7); // Remove "Bearer "
        String userId = redisTemplate.opsForValue().get("TEMP_LOGIN:" + tempToken);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired tempToken");
        }
        return userId;
    }

    @PostMapping("/send-otp")
    public String sendOtpTelegram(HttpServletRequest request) {
        String username = validateTempTokenAndGetUserId(request);
        String chatId = userService.getTelegramChatId(username);
        if (chatId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telegram not linked for user: " + username);
        }
        String otp = OtpGenerator.generateOtp();

        redisTemplate.opsForValue().set(
                "OTP:" + username,
                otp,
                5,
                TimeUnit.MINUTES
        );
        otpService.sendOtpTelegram(chatId, otp);
        return "OTP sent to your linked Telegram.";
    }

    @GetMapping("/generate-link-code")
    public Map<String, String> generateLinkCode(HttpServletRequest request) {
        String username = validateTempTokenAndGetUserId(request);
        String code = UUID.randomUUID().toString();
        log.info("user id {}", username);
        redisTemplate.opsForValue().set(code, username, 5, TimeUnit.MINUTES);
        return Map.of("code", code, "botUsername", "nvent_otp_generator_bot");
    }

    @GetMapping("/check-link")
    public boolean checkLink(HttpServletRequest request) {
        String username = validateTempTokenAndGetUserId(request);
        String chatId = userService.getTelegramChatId(username);
        log.info("chat id: [{}]", chatId);
        return StringUtils.isNotBlank(chatId);
    }
}