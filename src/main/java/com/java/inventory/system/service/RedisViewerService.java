package com.java.inventory.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.java.inventory.system.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RedisViewerService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public RedisViewerService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Map<String, Object> getAllKeyValues() {
        Set<String> keys = redisTemplate.keys("*");
        Map<String, Object> keyValueMap = new LinkedHashMap<>();

        if (keys != null) {
            for (String key : keys) {
                String rawValue = redisTemplate.opsForValue().get(key);
                if (rawValue == null) continue;

                try {
                    // If it's a quoted JSON string, unescape it first
                    if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
                        rawValue = objectMapper.readValue(rawValue, String.class);
                    }

                    JsonNode root = objectMapper.readTree(rawValue);

                    // Detect pattern ["java.util.ArrayList", [ {...}, {...} ]]
                    if (root.isArray() && root.size() == 2 &&
                            "java.util.ArrayList".equals(root.get(0).asText())) {

                        JsonNode productsNode = root.get(1);

                        List<Product> products = objectMapper.convertValue(
                                productsNode,
                                new TypeReference<List<Product>>() {}
                        );

                        keyValueMap.put(key, products);
                    } else {
                        // For anything else, just parse nicely
                        keyValueMap.put(key, objectMapper.readValue(rawValue, Object.class));
                    }
                } catch (Exception e) {
                    // Not JSON — just return raw
                    keyValueMap.put(key, rawValue);
                }
            }
        }

        return keyValueMap;
    }
}
