package com.java.inventory.system.authenticationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.inventory.system.constant.InventoryConstant;
import com.java.inventory.system.dto.AuthenticationRequest;
import com.java.inventory.system.dto.OtpVerificationRequest;
import com.java.inventory.system.repository.ProductRepository;
import com.java.inventory.system.repository.UserRepository;
import com.java.inventory.system.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@Sql("/testData/user.sql")
@ActiveProfiles("test")
public class LoginTest {

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

    @AfterEach
    void shutDown(
            @Autowired UserRepository userRepository
    ) throws IOException {
        userRepository.deleteAll();
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("loginUser")
    void loginTest() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setUsername("admin_user");
        authenticationRequest.setPassword("encoded_admin_pass1");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(authenticationRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError())
                .andDo(print());
    }
}

