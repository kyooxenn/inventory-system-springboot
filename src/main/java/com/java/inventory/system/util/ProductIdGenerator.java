package com.java.inventory.system.util;

import java.util.UUID;

public class ProductIdGenerator {

    public static String generateId() {
        // Generate a random UUID, remove dashes, take first 13 chars, convert to uppercase
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 13)
                .toUpperCase();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(generateId());
        }
    }
}
