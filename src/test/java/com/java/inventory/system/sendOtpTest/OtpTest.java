package com.java.inventory.system.sendOtpTest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.inventory.system.constant.InventoryConstant;
import com.java.inventory.system.dto.OtpVerificationRequest;
import com.java.inventory.system.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@ActiveProfiles("test")
public class OtpTest {

    @Autowired
    private MockMvc mockMvc;

    private static MockWebServer mockWebServer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;
    private String token;

    @Autowired
    private StringRedisTemplate redisTemplate;


    @BeforeEach
    void setUpMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8081);
        log.info("Mock server running at: {}", mockWebServer.url("/"));

        // Generate a valid JWT for testing
        token = "Bearer " + jwtUtil.generateToken("testuser", "ROLE_ADMIN");

        redisTemplate.opsForValue().set(
                "TEMP_LOGIN:" + "12345",
                "testuser",
                InventoryConstant.TEMP_TOKEN_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

    }

    @Test
    @DisplayName("sendOtpEmail")
    void sendOtpEmail() throws Exception {

        OtpVerificationRequest otpVerificationRequest = new OtpVerificationRequest();
        otpVerificationRequest.setTempToken("12345");
        otpVerificationRequest.setOtp("123456");
        otpVerificationRequest.setEmail("norbertbobila12@gmail.com");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/send-otp")
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(otpVerificationRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print());
    }
}
