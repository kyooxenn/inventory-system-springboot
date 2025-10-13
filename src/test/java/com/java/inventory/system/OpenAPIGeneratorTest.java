package com.java.inventory.system;

import com.java.inventory.system.security.JwtUtil;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class OpenAPIGeneratorTest {

    @Autowired
    protected MockMvc mockMvc;


    private static MockWebServer mockWebServer;

    @Autowired
    private JwtUtil jwtUtil;
    private String token;

    @BeforeEach
    void setUpMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8081);

        // Generate a valid JWT for testing
        token = "Bearer " + jwtUtil.generateToken("testuser", "ROLE_USER");
    }

    @AfterEach
    void shutDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void generateOpenAPIYaml() throws Exception {
        // Perform the request to fetch OpenAPI YAML
        MvcResult result = mockMvc.perform(get("/v3/api-docs.yaml")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        // Get response body (YAML content)
        String yamlContent = result.getResponse().getContentAsString();

        // Save to file
        File file = new File("src/test/resources/apidocs/rm_pull_create_openapi_spec.yaml");
        file.getParentFile().mkdirs(); // Ensure directory exists
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(yamlContent);
        }

        System.out.println("✅ OpenAPI YAML generated at: " + file.getAbsolutePath());
    }
}
