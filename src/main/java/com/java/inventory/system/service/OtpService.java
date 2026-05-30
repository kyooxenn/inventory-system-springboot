package com.java.inventory.system.service;

import com.java.inventory.system.constant.InventoryConstant;
import com.java.inventory.system.dto.OtpVerificationRequest;
import com.java.inventory.system.exception.BaseException;
import com.java.inventory.system.exception.ErrorType;
import com.java.inventory.system.model.AuthResponse;
import com.java.inventory.system.model.OtpLimit;
import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
import com.java.inventory.system.security.JwtUtil;
import com.java.inventory.system.util.OtpGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.java.inventory.system.exception.errortypes.NVentSvcErrorType.ERR_CLIENT_MAXIMUM_ATTEMPT;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    @Value("${BREVO_API_KEY}")
    private String apiKey;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final TelegramLongPollingBot telegramBot;
    private final CustomUserDetailsService userService; // For getting chatId

    public ResponseEntity<?> sendOtp(OtpVerificationRequest request, HttpServletRequest servletRequest) {
        if (StringUtils.isNotBlank(request.getTempToken())) {
            return sendOtpViaEmail(request);
        } else {
            return sentOtpViaTelegram(servletRequest);
        }
    }

    public ResponseEntity<?> sendOtpViaEmail(OtpVerificationRequest request) {
        String username = redisTemplate.opsForValue().get("TEMP_LOGIN:" + request.getTempToken());
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired session"));
        }

        OtpLimit otpLimit = checkAndLimitOtpResendAttempts(username);
        sendOtpEmail(request.getEmail(), generateAndStoreOtp(username));

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your registered email",
                "attempts_used", otpLimit.getAttempts() + 1,
                "attempts_remaining", Math.max(0, otpLimit.getMaxAttempts() - (otpLimit.getAttempts() + 1))

        ));
    }

    public ResponseEntity<?> sentOtpViaTelegram(HttpServletRequest servletRequest) {
        String username = validateTempTokenAndGetUserId(servletRequest);
        String chatId = userService.getTelegramChatId(username);

        if (chatId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Telegram not linked for user: " + username));
        }

        OtpLimit otpLimit = checkAndLimitOtpResendAttempts(username);
        sendOtpTelegram(chatId, generateAndStoreOtp(username));

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your linked Telegram account.",
                "attempts_used", otpLimit.getAttempts() + 1,
                "attempts_remaining", Math.max(0, otpLimit.getMaxAttempts() - (otpLimit.getAttempts() + 1))
        ));
    }

    public OtpLimit checkAndLimitOtpResendAttempts(String username) {
        // 🔒 Limit resend attempts to 3 per 10 minutes
        String attemptKey = "OTP_ATTEMPT:" + username;;

        OtpLimit otpLimit = new OtpLimit();

        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        int attempts = (attemptsStr != null) ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= otpLimit.getMaxAttempts()) {
            // 🕒 Get remaining cooldown time in seconds
            Long ttlSeconds = redisTemplate.getExpire(attemptKey, TimeUnit.SECONDS);

            if (ttlSeconds == null || ttlSeconds <= 0) {
                ttlSeconds = otpLimit.getCooldownMinutes() * 60;
            }

            long remainingMinutes = ttlSeconds / 60;
            long remainingSeconds = ttlSeconds % 60;

            throw createRateLimitException(ERR_CLIENT_MAXIMUM_ATTEMPT, remainingMinutes, remainingSeconds);
        }

        // ✅ Increment attempts count (expire after cooldown)
        redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, otpLimit.getCooldownMinutes(), TimeUnit.MINUTES);

        otpLimit.setAttempts(attempts);
        return otpLimit;
    }

    private BaseException createRateLimitException(ErrorType error, long minutes, long seconds) {
        String formattedMessage = String.format(error.getDesc(), minutes, seconds);
        log.error(formattedMessage);
        // Use reflection or create a new exception (if you have a constructor with message)
        return new BaseException(error) {
            @Override
            public String getMessage() {
                return formattedMessage;
            }
        };
    }

    public String generateAndStoreOtp(String username) {
        String otp = OtpGenerator.generateOtp();
        redisTemplate.opsForValue().set(
                "OTP:" + username,
                otp,
                InventoryConstant.OTP_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
        return otp;
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

    public void sendOtpEmail(String toEmail, String otp) {
        // 1. Build the request body according to Brevo's API spec
        Map<String, Object> requestBody = Map.of(
                "sender", Map.of(
                        "email", "norbertbobila12@gmail.com",  // Your verified sender email
                        "name", "From N-Vent"
                ),
                "to", List.of(
                        Map.of(
                                "email", toEmail,
                                "name", toEmail.split("@")[0]  // Extract name from email
                        )
                ),
                "subject", "Your One-Time Password (OTP)",
                "htmlContent", buildOtpHtmlContent(otp)
        );

        // 2. Initialize RestClient with Brevo authentication
        RestClient restClient = RestClient.builder()
                .baseUrl(BREVO_API_URL)
                .defaultHeader("api-key", apiKey)  // Note: "api-key" not "Authorization"
                .defaultHeader("Content-Type", "application/json")
                .build();

        try {
            // 3. Send the POST request to Brevo's transactional email endpoint
            String response = restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("OTP email sent successfully via Brevo to: {}. Response: {}", toEmail, response);
        } catch (Exception e) {
            log.error("Failed to send OTP email via Brevo to: {}. Error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }
    private String buildOtpHtmlContent(String otp) {
        return "<div style=\"font-family: Arial, sans-serif; text-align: center; padding: 20px;\">" +
                "<h2 style=\"color: #333;\">Your One-Time Password</h2>" +
                "<p style=\"font-size: 16px; color: #555;\">Use the code below to complete your verification:</p>" +
                "<div style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; margin: 20px 0; color: #000;\">" +
                otp +
                "</div>" +
                "<p style=\"font-size: 14px; color: #888;\">This code will expire in <b>5 minutes</b>.</p>" +
                "</div>";
    }

    public void sendOtpTelegram(String chatId, String otp) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId); // User's Telegram chat ID
        message.setText("Your OTP is: " + otp + ". It expires in 5 minutes.");

        try {
            telegramBot.execute(message);
            log.info("OTP sent successfully via telegram.");
        } catch (TelegramApiException e) {
            log.error("error: {}", e.getMessage());
        }
    }

    public ResponseEntity<?> validateOtp(OtpVerificationRequest request) {
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

            redisTemplate.delete("OTP_ATTEMPT:" + username);
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
