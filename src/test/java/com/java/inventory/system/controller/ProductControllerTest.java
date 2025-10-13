package com.java.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.inventory.system.dto.ProductRequest;
import com.java.inventory.system.repository.ProductRepository;
import com.java.inventory.system.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@Sql("/testData/product.sql")
class ProductControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private static MockWebServer mockWebServer;

    @Autowired
    private JwtUtil jwtUtil;
    private String token;

    @BeforeEach
    void setUpMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8081);
        log.info("Mock server running at: {}", mockWebServer.url("/"));

        // Generate a valid JWT for testing
        token = "Bearer " + jwtUtil.generateToken("testuser", "ROLE_USER");
    }

    @AfterEach
    void shutDown(
            @Autowired ProductRepository productRepository
    ) throws IOException {
        productRepository.deleteAll();
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Fetch all products successfully")
    void getAllProducts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/product")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("Retrieve a single product by product name")
    void getProduct() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/product/details/Steam Deck")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("Retrieve a single product by id")
    void getProductById() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/product/083FFA7C04C24")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("Create a new product successfully")
    void createProduct() throws Exception {

        ProductRequest request = new ProductRequest("FDS125G67J2SW","Rog Ally X", "Handheld PC", "Gadgets",
                BigDecimal.TEN,
                1,
                "Pieces"
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/product")
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("Update product details successfully")
    void updateProduct() throws Exception {

        ProductRequest request = new ProductRequest("083FFA7C04C24","Steam Deck 2", "Steam Handheld", "Gadgets", BigDecimal.TEN,
                1, "Pieces"
        );

        mockMvc.perform(MockMvcRequestBuilders.put("/v1/product/083FFA7C04C24")
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("Delete a product from inventory")
    void deleteProduct() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/product/083FFA7C04C24")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print());
    }
}