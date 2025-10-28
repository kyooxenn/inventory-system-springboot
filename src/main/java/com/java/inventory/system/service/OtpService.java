package com.java.inventory.system.service;

import com.java.inventory.system.dto.OtpVerificationRequest;
import com.java.inventory.system.model.AuthResponse;
import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
import com.java.inventory.system.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
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
    private final SecureRandom random = new SecureRandom();
    private static final long OTP_EXPIRATION_MINUTES = 2;

    public String generateOtp(String username, String email) throws IOException {
        String otp = String.format("%06d", random.nextInt(999999));

        redisTemplate.opsForValue().set(
                "OTP:" + username,
                otp,
                OTP_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        emailSenderService.sendOtpEmail(email, otp);

        log.info("OTP for " + username + ": " + otp);
        return otp;
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
