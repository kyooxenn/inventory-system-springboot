package com.java.inventory.system.service;

import com.java.inventory.system.dto.AuthRequest;
import com.java.inventory.system.model.AuthResponse;
import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
import com.java.inventory.system.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
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

    // Expiration time for temporary login session
    private static final long TEMP_TOKEN_EXPIRATION_MINUTES = 5;

    public ResponseEntity<?> login(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (BooleanUtils.isFalse(user.getIsVerified())) {

                String tempToken = UUID.randomUUID().toString();

                // ✅ Step 5: Store the username in Redis with expiration
                redisTemplate.opsForValue().set(
                        "TEMP_LOGIN:" + tempToken,
                        user.getUsername(),
                        TEMP_TOKEN_EXPIRATION_MINUTES,
                        TimeUnit.MINUTES
                );

                // ✅ Step 6: Return response (without JWT yet)
                return ResponseEntity.ok(Map.of(
                        "tempToken", tempToken,
                        "email", user.getEmail()
                ));

            } else {
                String jwt = jwtUtil.generateToken(request.getUsername(), user.getRoles());
                return ResponseEntity.ok(new AuthResponse(jwt));
            }

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
            // Check if username already exists
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Username already exists"));
            }

            // Create and save new user
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEmail(request.getEmail());
            user.setMobile(request.getMobile());
            user.setRoles("ROLE_USER");

            userRepository.save(user);

            // Return success response
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of("message", "User registered successfully"));

        } catch (Exception e) {
            // Return generic error response
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

}

