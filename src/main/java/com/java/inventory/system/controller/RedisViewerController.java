package com.java.inventory.system.controller;

import com.java.inventory.system.service.RedisViewerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class RedisViewerController {

    private final RedisViewerService redisViewerService;

    public RedisViewerController(RedisViewerService redisViewerService) {
        this.redisViewerService = redisViewerService;
    }

    @GetMapping("/redis/all")
    public Map<String, Object> showAllRedisValues() {
        return redisViewerService.getAllKeyValues();
    }
}
