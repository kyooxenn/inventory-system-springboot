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
    public ResponseEntity<?> sendOtpTelegram(HttpServletRequest request) {
        String username = validateTempTokenAndGetUserId(request);
        String chatId = userService.getTelegramChatId(username);

        if (chatId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Telegram not linked for user: " + username));
        }

        // 🔒 Limit resend attempts to 3 per 10 minutes
        String attemptKey = "OTP_ATTEMPT:" + username;;
        int maxAttempts = 3;
        long cooldownMinutes = 10;

        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        int attempts = (attemptsStr != null) ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= maxAttempts) {
            // 🕒 Get remaining cooldown time in seconds
            Long ttlSeconds = redisTemplate.getExpire(attemptKey, TimeUnit.SECONDS);

            if (ttlSeconds == null || ttlSeconds <= 0) {
                ttlSeconds = cooldownMinutes * 60;
            }

            long remainingMinutes = ttlSeconds / 60;
            long remainingSeconds = ttlSeconds % 60;

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", String.format(
                                    "Maximum resend attempts reached. Please try again in %d minutes and %d seconds.",
                                    remainingMinutes, remainingSeconds
                            )
                    ));
        }

        // ✅ Increment attempts count (expire after cooldown)
        redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, cooldownMinutes, TimeUnit.MINUTES);

        // 🔢 Generate OTP
        String otp = OtpGenerator.generateOtp();

        // 💾 Store OTP with expiration
        redisTemplate.opsForValue().set(
                "OTP:" + username,
                otp,
                InventoryConstant.OTP_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // 📩 Send OTP via Telegram
        otpService.sendOtpTelegram(chatId, otp);

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your linked Telegram account.",
                "attempts_used", attempts + 1,
                "attempts_remaining", Math.max(0, maxAttempts - (attempts + 1))
        ));
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