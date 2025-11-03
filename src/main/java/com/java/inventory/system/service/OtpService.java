package com.java.inventory.system.service;

import com.java.inventory.system.constant.InventoryConstant;
import com.java.inventory.system.dto.OtpVerificationRequest;
import com.java.inventory.system.model.AuthResponse;
import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
import com.java.inventory.system.security.JwtUtil;
import com.java.inventory.system.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final EmailSenderService emailSenderService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final TelegramLongPollingBot telegramBot;

    public ResponseEntity<?> sendOtpEmail(OtpVerificationRequest request) throws IOException {
        String username = redisTemplate.opsForValue().get("TEMP_LOGIN:" + request.getTempToken());

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired session"));
        }

        String attemptKey = "OTP_ATTEMPT:" + username;
        String attemptCountStr = redisTemplate.opsForValue().get(attemptKey);
        int attemptCount = attemptCountStr != null ? Integer.parseInt(attemptCountStr) : 0;

        if (attemptCount >= 3) {
            // 🔹 Get remaining cooldown time (in seconds)
            Long ttlSeconds = redisTemplate.getExpire(attemptKey, TimeUnit.SECONDS);
            long minutesLeft = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds / 60 : 0;
            long secondsLeft = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds % 60 : 0;

            String timeMessage = String.format(
                    "Maximum resend attempts reached. Please try again in %d minute%s and %d second%s.",
                    minutesLeft,
                    minutesLeft == 1 ? "" : "s",
                    secondsLeft,
                    secondsLeft == 1 ? "" : "s"
            );

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", timeMessage));
        }

        // 🔹 Generate OTP
        String otp = OtpGenerator.generateOtp();

        redisTemplate.opsForValue().set(
                "OTP:" + username,
                otp,
                InventoryConstant.OTP_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // 🔹 Increment attempt count with 10-minute expiry
        redisTemplate.opsForValue().set(
                attemptKey,
                String.valueOf(attemptCount + 1),
                10, // reset counter after 10 minutes
                TimeUnit.MINUTES
        );

        // emailSenderService.sendOtpEmail(request.getEmail(), otp);

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your registered email"
        ));
    }

    public void sendOtpTelegram(String chatId, String otp) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId); // User's Telegram chat ID
        message.setText("Your OTP is: " + otp + ". It expires in 5 minutes.");

        try {
            telegramBot.execute(message);
        } catch (TelegramApiException e) {
            log.error("error: {}", e.getMessage());
        }
    }


    public ResponseEntity<?> verifyOtp(OtpVerificationRequest request) {
        try {
            String username = redisTemplate.opsForValue().get("TEMP_LOGIN:" + request.getTempToken());

            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired session"));
            }

            boolean isValidOtp = validateOtp(username, request.getOtp());
            if (!isValidOtp) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid OTP"));
            }

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // verify user and give permission
            user.setIsVerified(Boolean.TRUE);
            user.setRoles("ROLE_ADMIN");
            userRepository.save(user);

            String jwt = jwtUtil.generateToken(username, user.getRoles());

            redisTemplate.delete("TEMP_LOGIN:" + request.getTempToken());

            return ResponseEntity.ok(new AuthResponse(jwt));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("OTP verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    public boolean validateOtp(String username, String otp) {
        String key = "OTP:" + username;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            // Remove OTP after successful verification
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
