package com.java.inventory.system.controller;

import com.java.inventory.system.constant.InventoryConstant;
import com.java.inventory.system.service.CustomUserDetailsService;
import com.java.inventory.system.service.OtpService;
import com.java.inventory.system.util.OtpGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@CrossOrigin
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramOtpController {

    private final OtpService otpService;
    private final StringRedisTemplate redisTemplate;
    private final CustomUserDetailsService userService; // For getting chatId

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
                InventoryConstant.OTP_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
        otpService.sendOtpTelegram(chatId, otp);
        return "OTP sent to your linked Telegram.";
    }

    @GetMapping("/generate-link-code")
    public Map<String, String> generateLinkCode(HttpServletRequest request) {
        String username = validateTempTokenAndGetUserId(request);
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(code, username, InventoryConstant.OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        return Map.of("code", code, "botUsername", "nvent_otp_generator_bot");
    }

    @GetMapping("/check-link")
    public boolean checkLink(HttpServletRequest request) {
        String username = validateTempTokenAndGetUserId(request);
        String chatId = userService.getTelegramChatId(username);
        return StringUtils.isNotBlank(chatId);
    }

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


    @GetMapping("/link-status/{code}")
    public ResponseEntity<?> getTelegramLinkStatus(@PathVariable String code) {
        String result = redisTemplate.opsForValue().get("linkResult:" + code);

        if (result == null) {
            return ResponseEntity.ok(Map.of("status", "pending"));
        }

        return ResponseEntity.ok(Map.of("status", result));
    }
}