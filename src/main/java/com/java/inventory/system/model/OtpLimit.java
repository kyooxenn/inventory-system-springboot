package com.java.inventory.system.model;

import lombok.Data;

@Data
public class OtpLimit {
    private int maxAttempts = 3;
    private long cooldownMinutes = 10L;
    private int attempts;
}
