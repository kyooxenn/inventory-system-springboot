package com.java.inventory.system.service;

import com.java.inventory.system.constant.InventoryConstant;
import com.java.inventory.system.dto.AuthRequest;
import com.java.inventory.system.model.AuthResponse;
import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
import com.java.inventory.system.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final BCryptPasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public ResponseEntity<?> login(AuthRequest request) {
        try {
            // check username and password input if valid else return BadCredentialsException
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            String tempToken = UUID.randomUUID().toString();

            redisTemplate.opsForValue().set(
                    "TEMP_LOGIN:" + tempToken,
                    user.getUsername(),
                    InventoryConstant.TEMP_TOKEN_EXPIRATION_MINUTES,
                    TimeUnit.MINUTES
            );

            // create temporary token after login (this will be used for otp verification)
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of(
                            "tempToken", tempToken,
                            "email", user.getEmail()
                    ));
        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        try {
            // 1. Check username and get the user if exists
            Optional<User> existingUserOpt = userRepository.findByUsername(request.getUsername());
            User targetUser = null;
            boolean isUpdate = false;

            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();
                if (BooleanUtils.isTrue(existingUser.getIsVerified())) {
                    return conflictResponse("Username already exists");
                }
                // Unverified user – we will update it
                targetUser = existingUser;
                isUpdate = true;
            }

            // 2. Uniqueness checks for email and mobile (excluding the target user if updating)
            Optional<User> emailUser = userRepository.findByEmail(request.getEmail());
            if (emailUser.isPresent() && (targetUser == null || !emailUser.get().getId().equals(targetUser.getId()))) {
                return conflictResponse("This email is already registered. Please use a different email address.");
            }

            Optional<User> mobileUser = userRepository.findByMobile(request.getMobile());
            if (mobileUser.isPresent() && (targetUser == null || !mobileUser.get().getId().equals(targetUser.getId()))) {
                return conflictResponse("This mobile is already registered. Please use a different mobile number.");
            }

            // 3. Create or update the user
            if (targetUser == null) {
                targetUser = new User();
                targetUser.setUsername(request.getUsername());
            }

            targetUser.setPassword(passwordEncoder.encode(request.getPassword()));
            targetUser.setEmail(request.getEmail());
            targetUser.setMobile(request.getMobile());
            if (targetUser.getRoles() == null) {
                targetUser.setRoles("ROLE_USER");
            }
            // isVerified remains false (for new or existing unverified user)

            userRepository.save(targetUser);

            // 4. Generate temporary verification token
            String tempToken = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                    "TEMP_LOGIN:" + tempToken,
                    targetUser.getUsername(),
                    InventoryConstant.TEMP_TOKEN_EXPIRATION_MINUTES,
                    TimeUnit.MINUTES
            );

            // 5. Return appropriate response
            HttpStatus status = isUpdate ? HttpStatus.OK : HttpStatus.CREATED;
            String message = isUpdate ?
                    "User updated and verification token resent. Please verify your account." :
                    "User registered successfully. Please verify your account.";

            return ResponseEntity.status(status).body(Map.of(
                    "message", message,
                    "tempToken", tempToken,
                    "email", targetUser.getEmail()
            ));

        } catch (DataIntegrityViolationException e) {
            // This handles rare race conditions where email/mobile was taken between check and save
            log.error("Data integrity violation during registration", e);
            return conflictResponse("Email or mobile number already exists. Please use different credentials.");
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred. Please try again later."));
        }
    }

    // Helper method for consistent conflict responses
    private ResponseEntity<Map<String, String>> conflictResponse(String errorMessage) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", errorMessage));
    }

}

