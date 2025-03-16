package com.java.inventory.system.util;

import java.time.Instant;

public class ProductIdGenerator {
    private static final long CUSTOM_EPOCH = 1704067200000L; // Custom Epoch (Jan 1, 2024)
    private static long lastTimestamp = -1L;
    private static long sequence = 0L;
    private static final int SEQUENCE_BITS = 12; // 12-bit sequence (0-4095)

    public static synchronized long generateId() {
        long timestamp = Instant.now().toEpochMilli() - CUSTOM_EPOCH;

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1); // Cycle within 0-4095
            if (sequence == 0) {
                while (timestamp <= lastTimestamp) {
                    timestamp = Instant.now().toEpochMilli() - CUSTOM_EPOCH;
                }
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;
        return (timestamp << SEQUENCE_BITS) | sequence;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(generateId());
        }
    }
}
